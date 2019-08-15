package io.dagger.hackernews.data.model

data class CommentItem(
    val item: Item,
    var child:List<CommentItem>?,
    var isExpanded:Boolean = false
)