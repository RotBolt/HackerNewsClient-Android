package io.dagger.hackernews.ui.news.newsDetails

import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.dagger.hackernews.R
import io.dagger.hackernews.data.model.CommentItem
import io.dagger.hackernews.utils.getDateTime
import io.dagger.hackernews.utils.invert
import kotlinx.android.synthetic.main.layout_comment.view.*
import kotlinx.android.synthetic.main.layout_comment_load.view.*

class CommentsAdapter(
    private val comments: List<CommentItem?>,
    private val childCommentListener: ChildCommentListener,
    private val depth: Int = 0
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_LOAD = 0
    private val VIEW_COMMENT = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_LOAD) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_comment_load, parent, false)
            LoadViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_comment, parent, false)
            CommentHolder(view)
        }
    }

    interface ChildCommentListener {
        fun onExpand(commentItem: CommentItem, rv: RecyclerView, loader: View, depth: Int)
        fun onCollapse(rv: RecyclerView, loader: View)
    }

    override fun getItemCount() = comments.size

    override fun getItemViewType(position: Int): Int {
        val item = comments[position]
        return if (item == null)
            VIEW_LOAD
        else
            VIEW_COMMENT
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CommentHolder -> {
                val item = comments[position]
                item?.let { holder.bind(it) }
            }
            is LoadViewHolder -> holder.bind()
        }
    }

    inner class LoadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind() {
            with(itemView) {
                val lf = LayoutInflater.from(context)
                for (i in 0 until depth) {
                    val view = lf.inflate(R.layout.seperator_view, commentLoadContainer, false)
                    divideContainerLoad.addView(view)
                }
            }
        }
    }

    inner class CommentHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(commentItem: CommentItem) {
            with(itemView) {
                tvAuthorComment.text = commentItem.item.by

                tvCommentStory.apply {
                    text = Html.fromHtml(commentItem.item.text)
                    movementMethod = LinkMovementMethod.getInstance()
                }

                tvTimeComment.text = getDateTime(commentItem.item.time)

                val lf = LayoutInflater.from(context)
                for (i in 0 until depth) {
                    val view = lf.inflate(R.layout.seperator_view, commentContainer, false)
                    divideContainer.addView(view)
                }

                val kids = commentItem.item.kids
                if (kids != null && kids.isNotEmpty()) {
                    tvChildren.text = "${kids.size} ${if (kids.size == 1) "comment" else "comments"}"

                    if (commentItem.isExpanded) {
                        ivExpand.invert()
                        childCommentListener.onExpand(commentItem, rvChildren, commentLoader, depth + 1)
                    }

                    subCommentContainer.setOnClickListener {
                        if (commentItem.isExpanded) {
                            ivExpand.invert()
                            childCommentListener.onCollapse(rvChildren, commentLoader)
                        } else {
                            ivExpand.invert()
                            Log.i("PUI","onExpand ${depth+1}")
                            childCommentListener.onExpand(commentItem, rvChildren, commentLoader, depth + 1)
                        }
                        commentItem.isExpanded = !commentItem.isExpanded
                    }
                } else {
                    tvChildren.isVisible = false
                    ivExpand.isVisible = false
                }
            }
        }

    }
}