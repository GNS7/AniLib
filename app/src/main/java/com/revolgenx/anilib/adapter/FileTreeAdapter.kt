package com.revolgenx.anilib.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.axet.androidlibrary.widgets.TreeListView
import com.github.axet.androidlibrary.widgets.TreeRecyclerView
import com.revolgenx.anilib.R
import com.revolgenx.anilib.adapter.meta.TorrentFile
import com.revolgenx.anilib.adapter.meta.TorrentFolder
import com.revolgenx.anilib.adapter.meta.TorrentName
import com.revolgenx.anilib.util.formatSize
import com.revolgenx.anilib.util.string
import kotlinx.android.synthetic.main.file_holder_adapter_layout.view.*
import kotlinx.android.synthetic.main.folder_holder_adapter_layout.view.*
import org.libtorrent4j.Priority
import org.libtorrent4j.TorrentHandle
import java.io.File
import java.util.*
import kotlin.Comparator


class FilesTreeAdapter(
    private var handle: TorrentHandle,
    private val totalCallback: ((total: String) -> Unit)? = null
) : TreeRecyclerView.TreeAdapter<FilesTreeAdapter.FileHolder>() {

    var folders = mutableMapOf<String, TorrentFolder>()

    private var receivedBytes: LongArray = longArrayOf()

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).nodes.isEmpty()) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileHolder {
        return FileHolder(
            LayoutInflater.from(parent.context).inflate(
                if (viewType == 0)
                    R.layout.file_holder_adapter_layout
                else
                    R.layout.folder_holder_adapter_layout, parent, false
            )
        )
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(holder: FileHolder, position: Int) {
        val item = getItem(holder.getAdapterPosition(this))
        holder.bind(item)
    }

    inner class FileHolder(private val v: View) : TreeRecyclerView.TreeHolder(v) {

        fun bind(treenode: TreeListView.TreeNode) {

            val item = treenode.tag
            if (item is TorrentFolder) {
                v.apply {
                    torrentFolderNameTv.text = item.name
                    torrentFolderCheckBox.setOnCheckedChangeListener(null)
                    torrentFolderCheckBox.isChecked = item.check
                    torrentFolderSizeTv.text = item.size.formatSize()
                    torrentFolderCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
                        item.check = isChecked
                        notifyDataSetChanged()
                    }
                    torrentFolderExpandIv.setImageResource(if (treenode.expanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down)
                }
            } else if (item is TorrentFile) {
                v.apply {
                    torrentFileNameTv.text = item.name
                    torrentFileCheck.setOnCheckedChangeListener(null)
                    torrentFileCheck.isChecked = item.priority != Priority.IGNORE
                    torrentFilePercentTv.text = string(R.string.percent_s)
                        .format(item.receivedBytes * 100f / item.size)
//                    torrentFilePercentTv.text =
//                        if (item.file!!.length > 0) {
//                            (item.file!!.bytesCompleted * 100f / item.file!!.length)).toString()+"%"
//                        } else "100%"

                    torrentFileCheck.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (isChecked) {
                            item.priority = Priority.DEFAULT
                        } else {
                            item.priority = Priority.IGNORE
                        }
                        notifyDataSetChanged()
                    }

                    torrentFileSizeTv.text = item.size.formatSize()

//                    if (item.parent == null) {
//                        setBackgroundColor(0)
//                    } else {
//                        setBackgroundColor(ThemeUtils.getColor(context, R.attr.fileBackgroundDark))
//                    }
                }
            }
        }
    }


    fun update(callback: () -> Unit) {
        if (!handle.status().hasMetadata()) return

        receivedBytes = handle.fileProgress(TorrentHandle.FileProgressFlags.PIECE_GRANULARITY)
        val count = handle.torrentFile().numFiles()
        val name = handle.name()
        if (root.nodes.size == 0 && count > 0) {
            root.nodes.clear()
            folders.clear()
            if (count == 1) {
                val f = TorrentFile(handle, 0)
                val n = TreeListView.TreeNode(root, f)
                f.node = n
                f.receivedBytes = receivedBytes[0]
                root.nodes.add(n)
            } else {
                for (i in 0 until count) {
                    val f = TorrentFile(handle, i)
                    val file = File(f.path)
                    f.receivedBytes = receivedBytes[i]
                    val parent = file.parent

                    if (parent != null) {
                        var folder = folders[parent]
                        if (folder == null) {
                            folder = TorrentFolder()
                            folder.path = parent
                            folder.name = folder.path!!
                            val n = TreeListView.TreeNode(root, folder)
                            folder.node = n
                            root.nodes.add(n)
                            folders[parent] = folder
                        }
                        folder.size += f.size
                        val n = TreeListView.TreeNode(folder.node, f)
                        f.node = n
                        folder.node!!.nodes.add(n)
                        f.parent = folder
                    }
                    if (f.parent == null) {
                        val n = TreeListView.TreeNode(root, f)
                        f.node = n
                        root.nodes.add(n)
                    }
                }
                for (n in root.nodes) {
                    if (n.tag is TorrentFolder) {
                        val m = n.tag as TorrentFolder
                        Collections.sort(m.node!!.nodes, SortFiles())
                    }
                }
                Collections.sort(root.nodes, SortFiles())
            }
        }

        callback.invoke()
    }


    fun checkAll(checked: Boolean) {
        if (!handle.status().hasMetadata()) return

        for (n in root.nodes) {
            if (n.tag is TorrentFolder) {
                val f = n.tag as TorrentFolder
                f.check = checked
            }
            if (n.tag is TorrentFile) {
                val f = n.tag as TorrentFile
                for (k in f.node!!.nodes) {
                    val m = k.tag as TorrentFile
                    m.priority =
                        if (checked) Priority.DEFAULT else Priority.IGNORE
                }
            }
        }

        updateTotal()
        notifyDataSetChanged()
    }

    fun updateTotal() {
        totalCallback?.invoke(handle.torrentFile().files().totalSize().formatSize())
    }

    fun updateItems() {
        if (!handle.status().hasMetadata() || !handle.isValid) return
        receivedBytes = handle.fileProgress(TorrentHandle.FileProgressFlags.PIECE_GRANULARITY)
        notifyDataSetChanged()
    }


    class SortFiles : Comparator<TreeListView.TreeNode> {
        override fun compare(file: TreeListView.TreeNode, file2: TreeListView.TreeNode): Int {
            val f1 = file.tag as TorrentName
            val f2 = file2.tag as TorrentName
            return f1.path!!.compareTo(f2.path!!)
        }
    }

}