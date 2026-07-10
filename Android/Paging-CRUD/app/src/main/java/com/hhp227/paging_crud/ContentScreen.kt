package com.hhp227.paging_crud

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hhp227.paging_crud.model.ListItem
import com.hhp227.paging_crud.ui.theme.PagingCRUDTheme
import com.hhp227.paging_crud.util.InjectorUtils
import com.hhp227.paging_crud.viewmodel.PostViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentScreen(
    modifier: Modifier = Modifier,
    viewModel: PostViewModel = viewModel(factory = InjectorUtils.providePostViewModelFactory()),
    onNavigateToCreate: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedPost by remember { mutableStateOf<ListItem.Post?>(null) }

    LaunchedEffect(state.message) {
        if (state.message.isNotEmpty()) {
            snackbarHostState.showSnackbar(state.message)
            viewModel.onMessageShown()
        }
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Paging CRUD") },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = "새로고침")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreate) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "글쓰기")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = state.itemList, key = ListItem.Post::id) { post ->
                    PostItem(post = post, onClick = { selectedPost = post })
                    HorizontalDivider()
                }
            }
            if (!state.isLoading && state.itemList.isEmpty()) {
                Text(
                    text = "게시물이 없습니다.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
    selectedPost?.also { post ->
        AlertDialog(
            onDismissRequest = { selectedPost = null },
            title = { Text(text = "게시글 삭제") },
            text = { Text(text = "이 게시글을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onDeletePost(post)
                        selectedPost = null
                    }
                ) {
                    Text(text = "삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPost = null }) {
                    Text(text = "취소")
                }
            }
        )
    }
}

@Composable
private fun PostItem(post: ListItem.Post, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = post.name ?: "익명", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = post.timeStamp ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = post.text, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Text(
                text = "댓글 ${post.replyCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "좋아요 ${post.likeCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PostItemPreview() {
    PagingCRUDTheme {
        PostItem(
            post = ListItem.Post(id = 1, name = "희표", text = "안녕하세요", timeStamp = "2026-07-11 00:00:00"),
            onClick = {}
        )
    }
}
