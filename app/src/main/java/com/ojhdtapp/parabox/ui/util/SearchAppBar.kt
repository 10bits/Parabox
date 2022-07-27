package com.ojhdtapp.parabox.ui.util

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ojhdtapp.parabox.domain.model.Contact

object SearchAppBar {
    const val NONE = 0
    const val SEARCH = 1
    const val SELECT = 2
    const val ARCHIVE_SELECT = 3
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchAppBar(
    modifier: Modifier = Modifier,
    activateState: Int = SearchAppBar.NONE,
    onActivateStateChanged: (value: Int) -> Unit,
    text: String,
    onTextChange: (text: String) -> Unit,
    placeholder: String,
    selection: SnapshotStateList<Contact> = mutableStateListOf(),
    onGroupAction: () -> Unit = {},
    onEditAction: () -> Unit = {},
    onExpandAction: () -> Unit = {},
    onNewTagAction: () -> Unit = {},
    onHideAction: () -> Unit = {},
    onPinAction: (value: Boolean) -> Unit = {},
    onArchiveAction: (value: Boolean) -> Unit = {},
    onMarkAsReadAction: (read: Boolean) -> Unit = {},
    sizeClass: WindowSizeClass,
    onMenuClick: () -> Unit,
) {
    val isActivated = activateState != SearchAppBar.NONE
    val isExpanded = sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val statusBarHeight = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(activateState) {
        if (activateState == SearchAppBar.SEARCH) focusRequester.requestFocus()
        else keyboardController?.hide()
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(
                statusBarHeight + 64.dp
            )
            .padding(
                PaddingValues(
                    animateDpAsState(targetValue = if (isActivated) 0.dp else 16.dp).value,
                    animateDpAsState(
                        targetValue = if (isActivated) 0.dp else 8.dp + statusBarHeight
                    ).value,
                    animateDpAsState(targetValue = if (isActivated) 0.dp else 16.dp).value,
                    animateDpAsState(targetValue = if (isActivated) 0.dp else 8.dp).value
                )
            ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(animateIntAsState(targetValue = if (isActivated) 0 else 50).value))
                .clickable {
                    if (activateState == SearchAppBar.NONE)
                        onActivateStateChanged(SearchAppBar.SEARCH)
                },
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                when (activateState) {
                    SearchAppBar.SELECT -> {
                        SelectContentField(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            isActivated = isActivated,
                            onActivateStateChanged = onActivateStateChanged,
                            selection = selection,
                            onGroupAction = onGroupAction,
                            onEditAction = onEditAction,
                            onNewTagAction = onNewTagAction,
                            onExpandAction = onExpandAction,
                            onPinAction = onPinAction,
                            onHideAction = onHideAction,
                            onArchiveAction = onArchiveAction,
                            onMarkAsReadAction = onMarkAsReadAction
                        )
                    }
                    SearchAppBar.SEARCH, SearchAppBar.NONE -> {
                        SearchContentField(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            isActivated = isActivated,
                            onActivateStateChanged = onActivateStateChanged,
                            placeholder = placeholder,
                            focusRequester = focusRequester,
                            text = text,
                            onTextChange = onTextChange,
                            keyboardController = keyboardController,
                            isExpanded = isExpanded,
                            onMenuClick = onMenuClick,
                        )
                    }
                    SearchAppBar.ARCHIVE_SELECT -> {
                        SelectSpecContentField(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            isActivated = isActivated,
                            onActivateStateChanged = onActivateStateChanged,
                            onHideAction = {}
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchContentField(
    modifier: Modifier = Modifier,
    isActivated: Boolean,
    onActivateStateChanged: (value: Int) -> Unit,
    placeholder: String,
    focusRequester: FocusRequester,
    text: String,
    onTextChange: (text: String) -> Unit,
    keyboardController: SoftwareKeyboardController?,
    isExpanded: Boolean,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(animateDpAsState(targetValue = if (isActivated) 64.dp else 48.dp).value),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                if (isExpanded) {
                    if (isActivated) {
                        onActivateStateChanged(SearchAppBar.NONE)
                    } else {
                        onMenuClick()
                    }
                } else {
                    onActivateStateChanged(
                        if (isActivated) SearchAppBar.NONE else SearchAppBar.SEARCH
                    )
                }
            },
        ) {
            Icon(
                imageVector = if (isActivated) Icons.Outlined.ArrowBack else (if (isExpanded) Icons.Outlined.Menu else Icons.Outlined.Search),
                contentDescription = "search",
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        BasicTextField(
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .clearFocusOnKeyboardDismiss(),
            value = text,
            onValueChange = { onTextChange(it.trim()) },
            enabled = isActivated,
            textStyle = MaterialTheme.typography.bodyLarge.merge(TextStyle(color = MaterialTheme.colorScheme.onSurface)),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { keyboardController?.hide() }),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (text.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                innerTextField()
            },
            cursorBrush = SolidColor(value = MaterialTheme.colorScheme.primary)
        )
        AnimatedVisibility(
            visible = !isActivated,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            IconButton(onClick = { /*TODO*/ }) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SelectContentField(
    modifier: Modifier = Modifier,
    isActivated: Boolean,
    onActivateStateChanged: (value: Int) -> Unit,
    selection: List<Contact>,
    onGroupAction: () -> Unit,
    onNewTagAction: () -> Unit,
    onEditAction: () -> Unit,
    onExpandAction: () -> Unit,
    onHideAction: () -> Unit = {},
    onPinAction: (value: Boolean) -> Unit = {},
    onArchiveAction: (value: Boolean) -> Unit = {},
    onMarkAsReadAction: (read: Boolean) -> Unit = {},
) {
    var expanded by remember {
        mutableStateOf(false)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(animateDpAsState(targetValue = if (isActivated) 64.dp else 48.dp).value),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onActivateStateChanged(SearchAppBar.NONE) },
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "close",
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        AnimatedContent(targetState = selection.size.toString(),
            transitionSpec = {
                // Compare the incoming number with the previous number.
                if (targetState > initialState) {
                    // If the target number is larger, it slides up and fades in
                    // while the initial (smaller) number slides up and fades out.
                    slideInVertically { height -> height } + fadeIn() with
                            slideOutVertically { height -> -height } + fadeOut()
                } else {
                    // If the target number is smaller, it slides down and fades in
                    // while the initial number slides down and fades out.
                    slideInVertically { height -> -height } + fadeIn() with
                            slideOutVertically { height -> height } + fadeOut()
                }.using(
                    // Disable clipping since the faded slide-in/out should
                    // be displayed out of bounds.
                    SizeTransform(clip = false)
                )
            }) { num ->
            Text(text = num, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.weight(1f))
        Crossfade(targetState = selection.size) {
            if (it > 1) {
                IconButton(onClick = onGroupAction) {
                    Icon(imageVector = Icons.Outlined.Group, contentDescription = "group")
                }
            } else if (it == 1) {
                IconButton(onClick = onEditAction) {
                    Icon(imageVector = Icons.Outlined.Info, contentDescription = "info")
                }
            } else {

            }
        }
        Crossfade(targetState = selection.size) {
            if (it >= 1) {
                Box(
                    modifier = Modifier
                        .wrapContentSize(Alignment.TopStart)
                ) {
                    IconButton(onClick = {
                        onExpandAction()
                        expanded = !expanded
                    }) {
                        Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "more")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        if (selection.map { it.isPinned }.contains(false)) {
                            DropdownMenuItem(
                                text = { Text(text = if (selection.size <= 1) "置顶" else "全部置顶") },
                                onClick = {
                                    onPinAction(true)
                                    expanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Flag,
                                        contentDescription = null
                                    )
                                })
                        } else {
                            DropdownMenuItem(
                                text = { Text(text = if (selection.size <= 1) "取消置顶" else "全部取消置顶") },
                                onClick = {
                                    onPinAction(false)
                                    expanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Flag,
                                        contentDescription = null
                                    )
                                })
                        }
                        DropdownMenuItem(
                            text = { Text(text = "隐藏会话") },
                            onClick = {
                                onHideAction()
                                expanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.HideSource,
                                    contentDescription = null
                                )
                            })
                        if (selection.map<Contact, Boolean> {
                                (it.latestMessage?.unreadMessagesNum?.compareTo(
                                    0
                                ) ?: 0) > 0
                            }.contains(false)) {
                            DropdownMenuItem(
                                text = { Text(text = "标记为未读") },
                                onClick = {
                                    onMarkAsReadAction(false)
                                    expanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.MarkChatUnread,
                                        contentDescription = null
                                    )
                                })
                        } else {
                            DropdownMenuItem(
                                text = { Text(text = "标记为已读") },
                                onClick = {
                                    onMarkAsReadAction(true)
                                    expanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.MarkChatRead,
                                        contentDescription = null
                                    )
                                })
                        }
                        if (selection.map { it.isArchived }.contains(false)) {

                            DropdownMenuItem(
                                text = { Text(text = if (selection.size <= 1) "归档" else "全部归档") },
                                onClick = {
                                    onArchiveAction(true)
                                    expanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Archive,
                                        contentDescription = null
                                    )
                                })
                        } else {

                            DropdownMenuItem(
                                text = { Text(text = if (selection.size <= 1) "取消归档" else "全部取消归档") },
                                onClick = {
                                    onArchiveAction(false)
                                    expanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Unarchive,
                                        contentDescription = null
                                    )
                                })
                        }
                        if (selection.size == 1) {
                            DropdownMenuItem(
                                text = { Text(text = "快速添加标签") },
                                onClick = {
                                    onNewTagAction()
                                    expanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.NewLabel,
                                        contentDescription = null
                                    )
                                })
                            DropdownMenuItem(
                                text = { Text(text = "详细信息") },
                                onClick = {
                                    onEditAction()
                                    expanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Info,
                                        contentDescription = null
                                    )
                                })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectSpecContentField(
    modifier: Modifier = Modifier,
    isActivated: Boolean,
    onActivateStateChanged: (value: Int) -> Unit,
    onHideAction: () -> Unit = {},
) {
    var expanded by remember {
        mutableStateOf(false)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(animateDpAsState(targetValue = if (isActivated) 64.dp else 48.dp).value),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onActivateStateChanged(SearchAppBar.NONE) },
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "close",
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = { onHideAction() }) {
            Icon(
                Icons.Outlined.HideSource,
                contentDescription = null
            )
        }
        Box(
            modifier = Modifier
                .wrapContentSize(Alignment.TopStart)
        ) {
            IconButton(onClick = {
                expanded = !expanded
            }) {
                Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "more")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(text = "移出所有归档") },
                    onClick = {
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Unarchive,
                            contentDescription = null
                        )
                    })
            }
        }
    }
}