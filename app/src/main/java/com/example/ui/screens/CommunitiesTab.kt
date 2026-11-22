package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Community
import com.example.data.model.Conversation
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SkeletonPostList
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightOutline
import com.example.ui.theme.LightSurface
import com.example.ui.theme.SkyMist
import com.example.ui.theme.SkyPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitiesTab(
    myProfile: UserProfile,
    communitiesState: UiState<List<Community>>,
    conversations: List<Conversation>,
    usersById: Map<String, UserProfile>,
    onCreateCommunity: (name: String, desc: String, announcement: String, members: List<String>, groupIds: List<String>) -> Unit,
    onCreateGroupInCommunity: (community: Community?, title: String, desc: String, members: List<String>) -> Unit,
    onOpenGroupChat: (String) -> Unit
) {
    var showCreateCommunitySheet by remember { mutableStateOf(false) }
    var targetCommunityForGroup by remember { mutableStateOf<Community?>(null) }
    var showNewStandaloneGroup by remember { mutableStateOf(false) }

    val communities = (communitiesState as? UiState.Success)?.data ?: emptyList()
    val groups = conversations.filter { it.type == "group" }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateCommunitySheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("create_community_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Create Community")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Text(
                    text = "Communities & Groups",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Organize related topic groups under unified communities",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when (communitiesState) {
                is UiState.Loading -> {
                    item {
                        SkeletonPostList(count = 3)
                    }
                }
                is UiState.Error -> {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.Hub,
                            title = "Could not load communities",
                            subtitle = communitiesState.message
                        )
                    }
                }
                is UiState.Success -> {
                    if (communities.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.Hub,
                                title = "Create your first Community",
                                subtitle = "Bring multiple groups together with announcements, shared admins, and instant member routing.",
                                actionLabel = "Create Community",
                                onAction = { showCreateCommunitySheet = true }
                            )
                        }
                    } else {
                        items(communities, key = { it.id }) { community ->
                            val linkedGroups = groups.filter { it.id in community.groupIds || it.communityId == community.id }
                            Card(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = LightSurface),
                                border = BorderStroke(1.2.dp, LightOutline),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Rounded.Hub, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(community.name, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                "${community.memberUids.size} members • ${linkedGroups.size} groups",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (community.announcementText.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = SkyMist,
                                            border = BorderStroke(1.dp, SkyPrimary.copy(alpha = 0.25f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Rounded.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(community.announcementText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    linkedGroups.forEach { grp ->
                                        Surface(
                                            onClick = { onOpenGroupChat(grp.id) },
                                            shape = RoundedCornerShape(12.dp),
                                            color = LightBackground,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Rounded.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(grp.title, style = MaterialTheme.typography.titleSmall)
                                                    Text(grp.lastMessageText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { targetCommunityForGroup = community },
                                        shape = CircleShape,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Group to Community")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                SectionHeader(
                    title = "All Groups (${groups.size})",
                    trailing = {
                        OutlinedButton(onClick = { showNewStandaloneGroup = true }, shape = CircleShape) {
                            Text("New Group")
                        }
                    }
                )
            }

            items(groups, key = { it.id }) { grp ->
                Card(
                    onClick = { onOpenGroupChat(grp.id) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = LightSurface),
                    border = BorderStroke(1.2.dp, LightOutline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(grp.title, style = MaterialTheme.typography.titleSmall)
                            Text("${grp.memberUids.size} members • ${grp.lastMessageText}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showCreateCommunitySheet) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var announcement by remember { mutableStateOf("") }
        val selectedMembers = remember { mutableStateListOf<String>() }
        val otherUsers = usersById.values.filter { it.userId != myProfile.userId }
        ModalBottomSheet(onDismissRequest = { showCreateCommunitySheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("New Community", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Community Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = announcement, onValueChange = { announcement = it }, label = { Text("Pinned Announcement") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(10.dp))
                if (otherUsers.isNotEmpty()) {
                    Text("Invite Members", style = MaterialTheme.typography.labelLarge)
                    LazyColumn(modifier = Modifier.height(140.dp)) {
                        items(otherUsers, key = { it.userId }) { u ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = u.userId in selectedMembers,
                                    onCheckedChange = { if (it) selectedMembers.add(u.userId) else selectedMembers.remove(u.userId) }
                                )
                                Text(u.displayName)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        onCreateCommunity(name, desc, announcement, selectedMembers.toList(), emptyList())
                        showCreateCommunitySheet = false
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Create Community") }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    val activeGroupTarget = targetCommunityForGroup
    if (activeGroupTarget != null || showNewStandaloneGroup) {
        var grpName by remember { mutableStateOf("") }
        var grpDesc by remember { mutableStateOf("") }
        val selectedMembers = remember { mutableStateListOf<String>() }
        val otherUsers = usersById.values.filter { it.userId != myProfile.userId }
        ModalBottomSheet(onDismissRequest = {
            targetCommunityForGroup = null
            showNewStandaloneGroup = false
        }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    if (activeGroupTarget != null) "Add Group to ${activeGroupTarget.name}" else "Create Group",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(value = grpName, onValueChange = { grpName = it }, label = { Text("Group Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = grpDesc, onValueChange = { grpDesc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.height(160.dp)) {
                    items(otherUsers, key = { it.userId }) { u ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = u.userId in selectedMembers,
                                onCheckedChange = { if (it) selectedMembers.add(u.userId) else selectedMembers.remove(u.userId) }
                            )
                            Text(u.displayName)
                        }
                    }
                }
                Button(
                    enabled = grpName.isNotBlank(),
                    onClick = {
                        onCreateGroupInCommunity(activeGroupTarget, grpName, grpDesc, selectedMembers.toList())
                        targetCommunityForGroup = null
                        showNewStandaloneGroup = false
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Create Group") }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
