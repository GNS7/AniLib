package com.revolgenx.anilib.torrent.core

import android.os.Parcel
import android.os.Parcelable
import com.revolgenx.anilib.event.TorrentDbEvent
import com.revolgenx.anilib.event.TorrentEvent
import com.revolgenx.anilib.event.TorrentEventType
import com.revolgenx.anilib.exception.TorrentException
import com.revolgenx.anilib.exception.TorrentPauseException
import com.revolgenx.anilib.exception.TorrentResumeException
import com.revolgenx.anilib.repository.db.entity.TorrentEntity
import com.revolgenx.anilib.torrent.state.TorrentState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.libtorrent4j.*
import org.libtorrent4j.TorrentStatus.*
import org.libtorrent4j.alerts.*
import org.libtorrent4j.swig.add_torrent_params
import timber.log.Timber
import java.io.File
import java.util.*
import kotlin.coroutines.CoroutineContext

typealias TorrentProgressListener = (() -> Unit)?

class Torrent() : Parcelable, KoinComponent, AlertListener {

    constructor(parcel: Parcel) : this() {
        hash = parcel.readString()!!
        handle = engine.find(Sha1Hash(hash))
        path = parcel.readString()!!
        createDate = Date(parcel.readLong())
        source = parcel.createByteArray()!!
        fastResumeData = parcel.createByteArray()!!
        hasError = parcel.readInt() == 1
        errorMsg = parcel.readString()!!
    }
    private var lastSaveResumeTime: Long = 0
    private val saveResumeSyncTime: Long = 10000 /* ms */

    var listeners = mutableListOf<TorrentProgressListener>()

    private val engine: TorrentEngine by inject()

    init {
        engine.addListener(this)
    }

    var hash: String = ""
    var errorMsg: String = ""
    var hasError = false
    var priorities: Array<Priority>? = null
        get() {
            return if (handle == null || handle?.isValid == false) null
            else if (handle!!.status().hasMetadata()) handle!!.filePriorities() else field
        }

    var fastResumeData: ByteArray = byteArrayOf()
    var corruptedResumeData = false
    var saveResumeCounter = 0
    var source: ByteArray = byteArrayOf()
        set(value) {
            if (value.isNotEmpty())
                field = value

            if (value.isNotEmpty() && handle == null) {
                handle = try {
                    engine.loadTorrent(
                        TorrentInfo(value),
                        File(path),
                        fastResumeData,
                        priorities,
                        null
                    )
                } catch (e: TorrentException) {
                    e.data as? TorrentHandle
                } catch (e: Exception) {
                    null
                }
            } else if (magnet.isNotEmpty() && handle == null) {
                handle = engine.loadTorrent(magnet, File(path))
            }
        }

    var handle: TorrentHandle? = null
    var magnet: String = ""

    var path: String = ""
    var name: String = ""
        get() {
            if (!checkValidity()) return field
            return handle!!.name()
        }

    var simpleState: Boolean = false //true running else false
    val progress: Float
        get() {
            if (handle == null) return 0f
            return handle!!.status().progress() * 100
        }

    var createDate: Date = Date(System.currentTimeMillis())
    val downloadSpeed: Long
        get() {
            if (!checkValidity()) return 0
            return handle!!.status().downloadRate().toLong()
        }

    val uploadSpeed: Long
        get() {
            if (!checkValidity()) return 0
            return handle!!.status().uploadRate().toLong()
        }

    val state: TorrentState
        get() {
            if (handle == null) return TorrentState.UNKNOWN

            val state = handle!!.status()
            return if (isPausedWithState() && state.isFinished)
                TorrentState.COMPLETED
            else if (isPausedWithState() && !state.isFinished)
                TorrentState.PAUSED
            else if (!isPausedWithState() && state.isFinished)
                TorrentState.SEEDING
            else when (state.state()) {
                State.CHECKING_FILES -> {
                    TorrentState.CHECKING_FILES
                }
                State.DOWNLOADING_METADATA -> {
                    TorrentState.DOWNLOADING_METADATA
                }
                State.DOWNLOADING -> {
                    TorrentState.DOWNLOADING
                }
                State.FINISHED -> {
                    TorrentState.COMPLETED
                }
                State.SEEDING -> {
                    TorrentState.SEEDING
                }
                State.ALLOCATING -> {
                    TorrentState.ALLOCATING
                }
                State.CHECKING_RESUME_DATA -> {
                    TorrentState.CHECKING_RESUME_DATA
                }
                State.UNKNOWN -> {
                    TorrentState.UNKNOWN
                }
                else -> {
                    TorrentState.UNKNOWN
                }
            }
        }

    var uploadLimit: Int = 0
        get() {
            return if (checkValidity()) {
                handle!!.uploadLimit
            } else 0
        }
        set(value) {
            field = value
            if (checkValidity()) {
                handle!!.uploadLimit = field
            }
        }

    var downloadLimit: Int = 0
        get() {
            return if (checkValidity()) {
                handle!!.downloadLimit
            } else 0
        }
        set(value) {
            field = value
            if (checkValidity()) {
                handle!!.downloadLimit = field
            }
        }

    val totalCompleted: Long
        get() {
            return if (!checkValidity()) 0
            else handle!!.status().allTimeDownload()
        }
    val totalSize: Long
        get() {
            if (handle == null) return 0
            return if (handle!!.status().hasMetadata()) {
                handle!!.torrentFile().totalSize()
            } else 0
        }

    fun connectedPeers() =
        if (!checkValidity()) 0
        else torrentStatus().numPeers()

    fun connectedSeeders() =
        if (!checkValidity()) 0
        else torrentStatus().numSeeds()

    fun connectedLeechers() =
        if (!checkValidity()) 0
        else connectedPeers() - connectedSeeders()


    fun totalPeers() =
        if (!checkValidity()) 0
        else (torrentStatus().numComplete() + torrentStatus().numIncomplete()).let { if (it > 0) it else torrentStatus().listPeers() }

    fun totalSeeders() =
        if (!checkValidity()) 0
        else torrentStatus().numComplete().let { if (it > 0) it else torrentStatus().listSeeds() }

    fun totalLeechers() =
        if (!checkValidity()) 0
        else torrentStatus().numIncomplete().let { if (it > 0) it else totalPeers() - totalSeeders() }


    fun checkValidity() = handle != null && handle?.isValid == true

    fun torrentStatus() = handle!!.status()

    fun eta(): Long {
        return if (!checkValidity() && state != TorrentState.DOWNLOADING && !torrentStatus().hasMetadata()) 0
        else if (torrentStatus().hasMetadata()) {
            handle!!.torrentFile().let {
                val left = it.totalSize() - torrentStatus().totalDone()
                val rate = torrentStatus().downloadPayloadRate()
                if (left <= 0 || rate <= 0) 0 else left / rate
            }
        } else 0
    }

    fun isPausedWithState(): Boolean {
        return isPaused() && !simpleState
    }

    fun isPaused(): Boolean {
        if (handle == null || handle?.isValid == false) return true
        return handle!!.isValid && (isPaused(handle!!.status(true)))
    }


    private fun isPaused(s: TorrentStatus): Boolean {
        return s.flags().and_(TorrentFlags.PAUSED).nonZero()
    }


    //TODO: CHECK FILE EXITS | CHECK STORAGE EJECTED CONDITION| READONLY | TORRENT_ALTERED | FREE_SPACE
    @Throws(TorrentResumeException::class)
    fun resume(force: Boolean = false) {
        if (!checkValidity()) throw TorrentResumeException("invalid torrent session", null)

        if (saveResumeCounter > 4 && !force)
            throw TorrentResumeException("Saving data, please wait", null)

//        if(!isPausedWithState()) return

        if (engine.torrentPreferenceModel.autoManaged)
            handle!!.setFlags(TorrentFlags.AUTO_MANAGED)
        else
            handle!!.unsetFlags(TorrentFlags.AUTO_MANAGED)

        try {
            hasError = false
            errorMsg = ""

            if (corruptedResumeData) {
                recheck()
            }

            handle!!.resume()
            saveResumeData(true)
        } catch (e: Exception) {
            throw TorrentResumeException("resume exception", e)
        }
    }

    @Throws(TorrentPauseException::class)
    fun pause(force: Boolean = false) {
        if (!checkValidity()) throw TorrentPauseException("invalid torrent session", null)

        if (saveResumeCounter > 4 && !force)
            throw TorrentPauseException("Saving data, please wait", null)


        handle!!.unsetFlags(TorrentFlags.AUTO_MANAGED)
        try {
            handle!!.pause()
            saveResumeData(true)
        } catch (e: Exception) {
            throw TorrentPauseException("pause exception", e)
        }
    }

    fun remove(withFiles: Boolean) {
        if (checkValidity()) {
            removeAllListener()
            if (withFiles)
                engine.remove(handle, SessionHandle.DELETE_FILES)
            else
                engine.remove(handle)

            removeEngineListener()
            handle = null
        }

    }

    fun update() {
        if (handle == null) return
        listeners.forEach { it?.invoke() }
    }


    /*
     * Generate fast-resume data for the torrent, see libtorrent documentation
     */
    private fun saveResumeData(force: Boolean) {
        val now = System.currentTimeMillis()
        if (force || now - lastSaveResumeTime >= saveResumeSyncTime) {
            lastSaveResumeTime = now
        } else { /* Skip, too fast, see SAVE_RESUME_SYNC_TIME */
            return
        }

        if (saveResumeCounter > 5) return

        try {
            if (checkValidity()) {
                Timber.d("saving start ${System.currentTimeMillis()}")
                handle!!.saveResumeData(TorrentHandle.SAVE_INFO_DICT)
                saveResumeCounter++
            }
        } catch (e: Exception) {
            Timber.w("%s:", "Error triggering resume data of " + hash)
            Timber.e(e)
        }
    }


    fun recheck() {
        if (handle == null) return
        handle!!.forceRecheck()
        update()
    }

    fun forceRecheck() {
        if (!checkValidity()) return
        handle!!.forceRecheck()
        resume(true)
        update()
    }


    override fun types(): IntArray = intArrayOf(
        AlertType.ADD_TORRENT.swig(),
        AlertType.METADATA_RECEIVED.swig(),
        AlertType.BLOCK_DOWNLOADING.swig(),
        AlertType.BLOCK_FINISHED.swig(),
        AlertType.STATE_UPDATE.swig(),
        AlertType.STATE_CHANGED.swig(),
        AlertType.TORRENT_CHECKED.swig(),
        AlertType.TORRENT_FINISHED.swig(),
        AlertType.TORRENT_REMOVED.swig(),
        AlertType.TORRENT_PAUSED.swig(),
        AlertType.TORRENT_RESUMED.swig(),
        AlertType.TORRENT_ERROR.swig(),
        AlertType.FASTRESUME_REJECTED.swig(),
        AlertType.STATS.swig(),
        AlertType.SAVE_RESUME_DATA.swig(),
        AlertType.SAVE_RESUME_DATA_FAILED.swig(),
        AlertType.STORAGE_MOVED.swig(),
        AlertType.STORAGE_MOVED_FAILED.swig(),
        AlertType.PIECE_FINISHED.swig(),
        AlertType.READ_PIECE.swig(),
        AlertType.METADATA_FAILED.swig(),
        AlertType.FILE_ERROR.swig()
    )


    override fun alert(alert: Alert<*>?) {

        if (alert !is TorrentAlert<*>) return

        if (handle == null) return
        if (!alert.handle().swig().op_eq(handle!!.swig())) return

        if (listeners.isEmpty()) return

        when (alert.type()) {
            AlertType.BLOCK_FINISHED -> {

            }
            AlertType.STATE_CHANGED -> {
                update()
            }
            AlertType.TORRENT_RESUMED -> {
                TorrentEvent(
                    listOf(this@Torrent),
                    TorrentEventType.TORRENT_RESUMED
                ).postSticky
                simpleState = true
                update()
            }
            AlertType.TORRENT_PAUSED -> {
                TorrentEvent(
                    listOf(this@Torrent),
                    TorrentEventType.TORRENT_PAUSED
                ).postSticky
                simpleState = false
                update()
            }
            AlertType.TORRENT_FINISHED -> {
                saveResumeData(true)
                TorrentEvent(listOf(this@Torrent), TorrentEventType.TORRENT_FINISHED).postEvent
                update()
            }
            AlertType.TORRENT_REMOVED -> {

            }

            AlertType.TORRENT_CHECKED -> {
                saveResumeData(true)
            }
            AlertType.STATS -> {
                update()
            }
            AlertType.SAVE_RESUME_DATA -> {
                corruptedResumeData = false
                saveResumeCounter--
                try {
                    Timber.d("saving done ${System.currentTimeMillis()}")
                    fastResumeData =
                        Vectors.byte_vector2bytes(add_torrent_params.write_resume_data((alert as SaveResumeDataAlert).params().swig()).bencode())
                    TorrentDbEvent(this).postEvent
                } catch (e: Exception) {

                }
            }
            AlertType.SAVE_RESUME_DATA_FAILED -> {
                saveResumeCounter--
            }

            AlertType.FASTRESUME_REJECTED -> {
                corruptedResumeData = true
            }

            AlertType.STORAGE_MOVED -> {
                saveResumeData(true)
                update()
            }
            AlertType.STORAGE_MOVED_FAILED -> {
                saveResumeData(true)
                update()
            }
            AlertType.PIECE_FINISHED -> {
                saveResumeData(false)
//                update()
            }
            AlertType.METADATA_RECEIVED -> {
                val metadataAlert = alert as MetadataReceivedAlert
                source = metadataAlert.torrentData()
                TorrentDbEvent(this).postEvent
                update()
            }
            else -> checkError(alert)
        }
    }

    private fun checkError(alert: Alert<*>) {
        when (alert.type()) {
            AlertType.TORRENT_ERROR -> {
                val errorAlert = alert as TorrentErrorAlert
                val error = errorAlert.error()
                if (error.isError) {
                    val filename = errorAlert.filename().substring(
                        errorAlert.filename().lastIndexOf("/") + 1
                    )
                    if (errorAlert.filename() != null) errorMsg = "[$filename] "
                    errorMsg += error.message() ?: ""
                    hasError = true
                    TorrentEvent(
                        listOf(this),
                        TorrentEventType.TORRENT_ERROR
                    ).postEvent
                    update()
                }
            }
            AlertType.METADATA_FAILED -> {
                val metadataFailedAlert = alert as MetadataFailedAlert
                val error = metadataFailedAlert.error
                if (error.isError) {
                    hasError = true
                    errorMsg = error.message() ?: ""
                    TorrentEvent(
                        listOf(this),
                        TorrentEventType.TORRENT_ERROR
                    ).postEvent
                    //todo :/ is it really needed for notification
                    update()
                }
            }
            AlertType.FILE_ERROR -> {
                val fileErrorAlert = alert as FileErrorAlert
                val error = fileErrorAlert.error()
                val filename = fileErrorAlert.filename().substring(
                    fileErrorAlert.filename().lastIndexOf("/") + 1
                )
                if (error.isError) {
                    hasError = true
                    errorMsg = "[" + filename + "] " + error.message()
                    TorrentEvent(
                        listOf(this),
                        TorrentEventType.TORRENT_ERROR
                    ).postEvent
                    update()
                }
            }
        }
    }

    override fun hashCode(): Int {
        return hash.hashCode()
    }

    fun toEntity(): TorrentEntity {
        return TorrentEntity(
            hash,
            path,
            magnet,
            createDate,
            fastResumeData,
            source,
            priorities,
            hasError,
            errorMsg
        )
    }


    fun addListener(torrentProgressListener: TorrentProgressListener) {
        listeners.add(torrentProgressListener)
    }

    fun addEngineListener() {
        engine.addListener(this)
    }

    fun removeEngineListener() {
        engine.removeListener(this)
    }

    fun removeListener(torrentProgressListener: TorrentProgressListener) {
        listeners.remove(torrentProgressListener)
        Timber.d("add listener ${listeners.size}")
    }

    fun removeAllListener() {
        listeners.clear()
    }


    fun forceReannounce() {
        if (!checkValidity()) return
        handle!!.forceReannounce()
    }

    fun torrentAddTracker(list: List<AnnounceEntry>) {
        if (!checkValidity()) return
        list.forEach {
            handle!!.addTracker(it)
        }
    }


    override fun equals(other: Any?): Boolean {
        return if ((other is Torrent)) {
            other.hash == hash
        } else false
    }


    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.apply {
            writeString(hash)
            writeString(path)
            writeLong(createDate.time)
            writeByteArray(source)
            writeByteArray(fastResumeData)
            writeInt(if (hasError) 1 else 0)
            writeString(errorMsg)
        }
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Torrent> {
        const val SAVE_RESUME_SYNC_TIME = 10000L


        override fun createFromParcel(parcel: Parcel): Torrent {
            return Torrent(parcel)
        }

        override fun newArray(size: Int): Array<Torrent?> {
            return arrayOfNulls(size)
        }
    }
}