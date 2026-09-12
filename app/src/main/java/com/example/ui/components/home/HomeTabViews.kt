package com.example.ui.components.home

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ExportedVideoEntity
import com.example.data.local.ProjectEntity
import com.example.data.presets.TemplatesCatalog
import com.example.data.presets.VideoTemplate
import com.example.domain.StudioAccountManager
import com.example.domain.UserProfile
import com.example.domain.model.AspectRatio
import com.example.ui.AppScreen
import com.example.ui.StudioViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================================
// 1. TEMPLATES TAB VIEW
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTemplatesTabView(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val savedTemplateIds by StudioAccountManager.savedTemplateIds.collectAsState()
  val customTemplates by StudioAccountManager.customTemplates.collectAsState()

  var searchQuery by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableStateOf("All") } // "All", "Trending", "Saved", "My Created"
  var showCreateTemplateDialog by remember { mutableStateOf(false) }
  var previewTemplate by remember { mutableStateOf<VideoTemplate?>(null) }

  val allAvailableTemplates = remember(customTemplates) {
    customTemplates + TemplatesCatalog.templates
  }

  val filteredTemplates = remember(allAvailableTemplates, searchQuery, selectedFilter, savedTemplateIds) {
    allAvailableTemplates.filter { tpl ->
      val matchesSearch = searchQuery.isBlank() ||
        tpl.title.contains(searchQuery, ignoreCase = true) ||
        tpl.category.contains(searchQuery, ignoreCase = true) ||
        tpl.description.contains(searchQuery, ignoreCase = true)

      val matchesFilter = when (selectedFilter) {
        "Trending" -> tpl.isPro || tpl.id.contains("reels") || tpl.id.contains("yt")
        "Saved" -> savedTemplateIds.contains(tpl.id)
        "My Created" -> tpl.category == "User-Created" || tpl.id.startsWith("cust_")
        else -> true
      }

      matchesSearch && matchesFilter
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    // Top Bar Header & Action
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Template Studio",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 22.sp
          )
        )
        Text(
          text = "Trending motion presets & custom auto-saving templates",
          style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
        )
      }

      Button(
        onClick = { showCreateTemplateDialog = true },
        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = Modifier.testTag("create_custom_template_button")
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Create", fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }
    }

    // Search Bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("Search templates, categories, motion styles...", color = TextTertiary, fontSize = 13.sp) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { searchQuery = "" }) {
            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
          }
        }
      },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp)
        .testTag("templates_search_field"),
      shape = RoundedCornerShape(14.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = StudioSurface,
        unfocusedContainerColor = StudioSurface,
        focusedBorderColor = CyanAccent,
        unfocusedBorderColor = StudioBorder,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
      )
    )

    // Filter Chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf("All", "Trending", "Saved", "My Created").forEach { filterName ->
        val isSelected = selectedFilter == filterName
        FilterChip(
          selected = isSelected,
          onClick = { selectedFilter = filterName },
          label = {
            Text(
              text = when (filterName) {
                "Trending" -> "🔥 Trending"
                "Saved" -> "⭐ Saved (${savedTemplateIds.size})"
                "My Created" -> "✏️ My Created (${customTemplates.size})"
                else -> "All Templates"
              },
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CyanAccent.copy(alpha = 0.2f),
            selectedLabelColor = CyanAccent,
            containerColor = StudioSurface,
            labelColor = TextSecondary
          ),
          border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            selectedBorderColor = CyanAccent,
            borderColor = StudioBorder
          )
        )
      }
    }

    // Templates List / Grid
    if (filteredTemplates.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Outlined.Style, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(56.dp))
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = if (searchQuery.isNotBlank()) "No matching templates" else "No templates in this category",
            style = MaterialTheme.typography.titleMedium.copy(color = TextSecondary)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Try clearing your search query or tap Create to make a new template",
            style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary, fontSize = 12.sp)
          )
        }
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(bottom = 12.dp)
      ) {
        items(filteredTemplates, key = { it.id }) { tpl ->
          val isSaved = savedTemplateIds.contains(tpl.id)

          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .border(
                width = 1.dp,
                color = if (isSaved) AmberAccent.copy(alpha = 0.6f) else StudioBorder,
                shape = RoundedCornerShape(16.dp)
              )
              .clickable { previewTemplate = tpl }
              .testTag("template_card_${tpl.id}"),
            colors = CardDefaults.cardColors(containerColor = StudioSurface)
          ) {
            Column {
              // Thumbnail Header Box
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(110.dp)
                  .background(
                    Brush.linearGradient(
                      listOf(
                        Color(tpl.thumbnailGradientStart),
                        Color(tpl.thumbnailGradientEnd)
                      )
                    )
                  )
                  .padding(10.dp)
              ) {
                // Top Row: Emoji Icon + Bookmark Star
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.size(28.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(tpl.iconEmoji, fontSize = 14.sp)
                    }
                  }

                  IconButton(
                    onClick = {
                      StudioAccountManager.toggleSavedTemplate(tpl.id)
                      Toast.makeText(
                        context,
                        if (isSaved) "Removed from Saved Templates" else "Saved to My Templates ⭐",
                        Toast.LENGTH_SHORT
                      ).show()
                    },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(
                      imageVector = if (isSaved) Icons.Default.Star else Icons.Outlined.StarBorder,
                      contentDescription = "Save Template",
                      tint = if (isSaved) AmberAccent else Color.White
                    )
                  }
                }

                // Bottom Aspect Ratio Badge
                Surface(
                  modifier = Modifier.align(Alignment.BottomStart),
                  shape = RoundedCornerShape(6.dp),
                  color = Color.Black.copy(alpha = 0.6f)
                ) {
                  Text(
                    text = "${tpl.aspectRatio.label} • ${tpl.durationMs / 1000}s",
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = Color.White,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              // Details Body
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = tpl.title,
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.sp
                  ),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = tpl.category,
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                  )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                  onClick = {
                    viewModel.applyTemplate(tpl)
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .testTag("use_template_btn_${tpl.id}")
                ) {
                  Text("Use Template", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
              }
            }
          }
        }
      }
    }
  }

  // Create Custom Template Modal Dialog
  if (showCreateTemplateDialog) {
    CreateTemplateModalDialog(
      onDismiss = { showCreateTemplateDialog = false },
      onCreate = { title, category, description, durationSec, aspect ->
        val newTpl = StudioAccountManager.createAndSaveCustomTemplate(
          title = title,
          category = category,
          description = description,
          durationSec = durationSec,
          aspectRatio = aspect
        )
        Toast.makeText(context, "Custom Template \"${newTpl.title}\" saved successfully!", Toast.LENGTH_SHORT).show()
        showCreateTemplateDialog = false
      }
    )
  }

  // Template Preview Inspector Modal
  previewTemplate?.let { tpl ->
    TemplatePreviewModalDialog(
      template = tpl,
      isSaved = savedTemplateIds.contains(tpl.id),
      onToggleSave = { StudioAccountManager.toggleSavedTemplate(tpl.id) },
      onUseTemplate = {
        previewTemplate = null
        viewModel.applyTemplate(tpl)
      },
      onDismiss = { previewTemplate = null }
    )
  }
}

// ============================================================================
// 2. PROJECTS TAB VIEW
// ============================================================================
@Composable
fun HomeProjectsTabView(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val projects by viewModel.allProjects.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  var selectedTab by remember { mutableStateOf("All Projects") } // "All Projects", "Drafts", "Recent"
  var renameProjectTarget by remember { mutableStateOf<ProjectEntity?>(null) }
  var deleteProjectTarget by remember { mutableStateOf<ProjectEntity?>(null) }

  val filteredProjects = remember(projects, searchQuery, selectedTab) {
    projects.filter { project ->
      val matchesSearch = searchQuery.isBlank() || project.name.contains(searchQuery, ignoreCase = true)
      val matchesTab = when (selectedTab) {
        "Drafts" -> project.isDraft
        "Recent" -> System.currentTimeMillis() - project.lastEditedTime < 7 * 24 * 3600 * 1000L
        else -> true
      }
      matchesSearch && matchesTab
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    // Top Bar Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Projects & Drafts",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 22.sp
          )
        )
        Text(
          text = "${projects.size} total project${if (projects.size == 1) "" else "s"} saved locally",
          style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
        )
      }
    }

    // Search Bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("Search projects by name...", color = TextTertiary, fontSize = 13.sp) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { searchQuery = "" }) {
            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
          }
        }
      },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp)
        .testTag("projects_tab_search_field"),
      shape = RoundedCornerShape(14.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = StudioSurface,
        unfocusedContainerColor = StudioSurface,
        focusedBorderColor = CyanAccent,
        unfocusedBorderColor = StudioBorder,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
      )
    )

    // Filter Chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf("All Projects", "Drafts", "Recent").forEach { tab ->
        val isSelected = selectedTab == tab
        FilterChip(
          selected = isSelected,
          onClick = { selectedTab = tab },
          label = {
            Text(
              text = when (tab) {
                "Drafts" -> "📝 Drafts (${projects.count { it.isDraft }})"
                "Recent" -> "⏱️ Recent"
                else -> "All Projects (${projects.size})"
              },
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = StudioSurfaceVariant,
            selectedLabelColor = CyanAccent,
            containerColor = StudioSurface,
            labelColor = TextSecondary
          )
        )
      }
    }

    // Project List
    if (filteredProjects.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Outlined.Folder, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(56.dp))
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = if (searchQuery.isNotBlank()) "No matching projects" else "No saved projects yet",
            style = MaterialTheme.typography.titleMedium.copy(color = TextSecondary)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Tap Start Project in the navigation bar to create a new timeline",
            style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary, fontSize = 12.sp)
          )
        }
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(bottom = 12.dp)
      ) {
        items(filteredProjects, key = { it.id }) { project ->
          val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }
          val formattedDate = remember(project.lastEditedTime) { dateFormat.format(Date(project.lastEditedTime)) }

          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
              .clickable { viewModel.loadProject(project) }
              .testTag("project_item_${project.id}"),
            colors = CardDefaults.cardColors(containerColor = StudioSurface)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Thumbnail Box
              Box(
                modifier = Modifier
                  .size(70.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(
                    Brush.linearGradient(
                      listOf(
                        Color(0xFF1E293B),
                        Color(0xFF0F172A)
                      )
                    )
                  )
                  .border(1.dp, CyanAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Movie, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(28.dp))
                if (project.isDraft) {
                  Surface(
                    color = AmberAccent,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                      .align(Alignment.TopStart)
                      .padding(4.dp)
                  ) {
                    Text(
                      text = "DRAFT",
                      style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.Black,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                      ),
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.width(14.dp))

              // Details
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = project.name,
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 15.sp
                  ),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                  text = formattedDate,
                  style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary, fontSize = 11.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = StudioSurfaceVariant
                  ) {
                    Text(
                      text = project.aspectRatio,
                      style = MaterialTheme.typography.labelSmall.copy(
                        color = CyanAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                      ),
                      modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                  }
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = StudioSurfaceVariant
                  ) {
                    Text(
                      text = "${project.durationMs / 1000}s",
                      style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontSize = 10.sp
                      ),
                      modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                  }
                }
              }

              // Actions Menu
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                  onClick = { viewModel.duplicateProject(project.id) },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(
                  onClick = { renameProjectTarget = project },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(Icons.Default.Edit, contentDescription = "Rename", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(
                  onClick = { deleteProjectTarget = project },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseAccent, modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }
      }
    }
  }

  // Rename Dialog
  renameProjectTarget?.let { project ->
    var newName by remember { mutableStateOf(project.name) }
    AlertDialog(
      onDismissRequest = { renameProjectTarget = null },
      title = { Text("Rename Project", color = TextPrimary) },
      text = {
        OutlinedTextField(
          value = newName,
          onValueChange = { newName = it },
          label = { Text("Project Title") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanAccent,
            unfocusedBorderColor = StudioBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          )
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (newName.isNotBlank()) {
              viewModel.renameProject(project.id, newName.trim())
            }
            renameProjectTarget = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { renameProjectTarget = null }) {
          Text("Cancel", color = TextSecondary)
        }
      },
      containerColor = StudioSurface
    )
  }

  // Delete Dialog
  deleteProjectTarget?.let { project ->
    AlertDialog(
      onDismissRequest = { deleteProjectTarget = null },
      title = { Text("Delete Project?", color = TextPrimary) },
      text = { Text("Are you sure you want to delete \"${project.name}\"? This action cannot be undone.", color = TextSecondary) },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteProject(project.id)
            deleteProjectTarget = null
            Toast.makeText(context, "Project deleted", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = RoseAccent, contentColor = Color.White)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { deleteProjectTarget = null }) {
          Text("Cancel", color = TextSecondary)
        }
      },
      containerColor = StudioSurface
    )
  }
}

// ============================================================================
// 3. VIDEO CLIPS TAB VIEW
// ============================================================================
@Composable
fun HomeVideoClipsTabView(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val exportedVideos by viewModel.exportedVideos.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableStateOf("All Clips") }
  var previewVideo by remember { mutableStateOf<ExportedVideoEntity?>(null) }
  var deleteVideoTarget by remember { mutableStateOf<ExportedVideoEntity?>(null) }

  val filteredVideos = remember(exportedVideos, searchQuery, selectedFilter) {
    exportedVideos.filter { video ->
      val matchesSearch = searchQuery.isBlank() || video.title.contains(searchQuery, ignoreCase = true)
      val matchesFilter = when (selectedFilter) {
        "4K / 1080p" -> video.resolution.contains("1080") || video.resolution.contains("4K")
        else -> true
      }
      matchesSearch && matchesFilter
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    // Top Bar Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Video Clips & Media",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 22.sp
          )
        )
        Text(
          text = "${exportedVideos.size} exported video clip${if (exportedVideos.size == 1) "" else "s"} available",
          style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
        )
      }
    }

    // Search Bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("Search video clips by name...", color = TextTertiary, fontSize = 13.sp) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { searchQuery = "" }) {
            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
          }
        }
      },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp)
        .testTag("video_clips_search_field"),
      shape = RoundedCornerShape(14.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = StudioSurface,
        unfocusedContainerColor = StudioSurface,
        focusedBorderColor = CyanAccent,
        unfocusedBorderColor = StudioBorder,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
      )
    )

    // Filter Chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf("All Clips", "4K / 1080p").forEach { filter ->
        val isSelected = selectedFilter == filter
        FilterChip(
          selected = isSelected,
          onClick = { selectedFilter = filter },
          label = { Text(filter, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = StudioSurfaceVariant,
            selectedLabelColor = CyanAccent,
            containerColor = StudioSurface,
            labelColor = TextSecondary
          )
        )
      }
    }

    // Grid of Clips
    if (filteredVideos.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Outlined.OndemandVideo, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(56.dp))
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = if (searchQuery.isNotBlank()) "No matching video clips" else "No exported video clips yet",
            style = MaterialTheme.typography.titleMedium.copy(color = TextSecondary)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Export video projects from the timeline to see clips here",
            style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary, fontSize = 12.sp)
          )
        }
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(bottom = 12.dp)
      ) {
        items(filteredVideos, key = { it.id }) { video ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
              .clickable { previewVideo = video }
              .testTag("video_clip_card_${video.id}"),
            colors = CardDefaults.cardColors(containerColor = StudioSurface)
          ) {
            Column {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(100.dp)
                  .background(
                    Brush.linearGradient(
                      listOf(
                        Color(0xFF0F2027),
                        Color(0xFF203A43),
                        Color(0xFF2C5364)
                      )
                    )
                  )
                  .padding(8.dp),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.PlayCircleFilled, contentDescription = "Play", tint = CyanAccent, modifier = Modifier.size(36.dp))

                Surface(
                  modifier = Modifier.align(Alignment.BottomEnd),
                  shape = RoundedCornerShape(4.dp),
                  color = Color.Black.copy(alpha = 0.7f)
                ) {
                  Text(
                    text = "${video.durationMs / 1000}s",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontSize = 10.sp),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                  )
                }
              }

              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = video.title,
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 13.sp
                  ),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "${video.resolution} • ${video.fps} FPS",
                  style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary, fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  IconButton(
                    onClick = { previewVideo = video },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = CyanAccent, modifier = Modifier.size(20.dp))
                  }

                  IconButton(
                    onClick = { deleteVideoTarget = video },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseAccent, modifier = Modifier.size(18.dp))
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Delete Video Dialog
  deleteVideoTarget?.let { video ->
    AlertDialog(
      onDismissRequest = { deleteVideoTarget = null },
      title = { Text("Delete Clip?", color = TextPrimary) },
      text = { Text("Remove clip \"${video.title}\" from studio storage?", color = TextSecondary) },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteExportedVideo(video.id)
            deleteVideoTarget = null
            Toast.makeText(context, "Video clip deleted", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = RoseAccent, contentColor = Color.White)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { deleteVideoTarget = null }) {
          Text("Cancel", color = TextSecondary)
        }
      },
      containerColor = StudioSurface
    )
  }

  // Preview Video Dialog
  previewVideo?.let { video ->
    AlertDialog(
      onDismissRequest = { previewVideo = null },
      title = { Text(video.title, color = TextPrimary) },
      text = {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(180.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color.Black),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Movie, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(48.dp))
          }
          Spacer(modifier = Modifier.height(12.dp))
          Text("Resolution: ${video.resolution}", color = TextSecondary, fontSize = 12.sp)
          Text("Duration: ${video.durationMs / 1000} seconds", color = TextSecondary, fontSize = 12.sp)
          Text("Path: ${video.filePath.takeLast(35)}", color = TextTertiary, fontSize = 10.sp)
        }
      },
      confirmButton = {
        Button(
          onClick = { previewVideo = null },
          colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
        ) {
          Text("Close")
        }
      },
      containerColor = StudioSurface
    )
  }
}

// ============================================================================
// 4. ME / MY ACCOUNT TAB VIEW
// ============================================================================
@Composable
fun HomeAccountTabView(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val profile by StudioAccountManager.profile.collectAsState()
  val socialConnections by StudioAccountManager.socialConnections.collectAsState()
  val userAccounts by StudioAccountManager.userAccounts.collectAsState()
  val projects by viewModel.allProjects.collectAsState()
  val exportedVideos by viewModel.exportedVideos.collectAsState()
  val savedTemplates by StudioAccountManager.savedTemplateIds.collectAsState()

  var showEditProfileDialog by remember { mutableStateOf(false) }
  var showFeedbackDialog by remember { mutableStateOf(false) }
  var showTermsDialog by remember { mutableStateOf(false) }
  var showPrivacyDialog by remember { mutableStateOf(false) }
  var showSwitchAccountDialog by remember { mutableStateOf(false) }
  var showLogOutDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Top Bar Header & Settings Action
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "My Account Center",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              color = TextPrimary,
              fontSize = 22.sp
            )
          )
          Text(
            text = "Manage your profile, connected accounts & studio settings",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
          )
        }

        IconButton(
          onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
          modifier = Modifier
            .clip(CircleShape)
            .background(StudioSurface)
            .testTag("account_settings_top_button")
        ) {
          Icon(Icons.Default.Settings, contentDescription = "Settings", tint = CyanAccent)
        }
      }
    }

    // User Profile Header Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .border(1.dp, StudioBorder, RoundedCornerShape(20.dp))
          .testTag("user_profile_card"),
        colors = CardDefaults.cardColors(containerColor = StudioSurface)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(profile.avatarColor)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = profile.name.take(2).uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Bold,
                  color = Color.Black,
                  fontSize = 22.sp
                )
              )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = profile.name,
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 17.sp
                  )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = AmberAccent
                ) {
                  Text(
                    text = "PRO VIP",
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = Color.Black,
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 9.sp
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Text(
                text = profile.handle,
                style = MaterialTheme.typography.bodySmall.copy(color = CyanAccent, fontSize = 12.sp)
              )
              Text(
                text = profile.bio,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Stats Bar Row
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(StudioSurfaceVariant)
              .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            AccountStatItem("Projects", "${projects.size}")
            AccountStatItem("Clips", "${exportedVideos.size}")
            AccountStatItem("Templates", "${savedTemplates.size}")
            AccountStatItem("Storage", "1.2 GB")
          }

          Spacer(modifier = Modifier.height(12.dp))

          Button(
            onClick = { showEditProfileDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceVariant, contentColor = TextPrimary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("edit_profile_btn")
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Edit Profile & Account", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }
      }
    }

    // Social Accounts Connection Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .border(1.dp, StudioBorder, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = StudioSurface)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Connected Social Accounts",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
          )
          Text(
            text = "Direct 1-tap export to YouTube, TikTok & Reels",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
          )

          Spacer(modifier = Modifier.height(12.dp))

          socialConnections.forEach { conn ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(conn.platformIcon, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(conn.platformName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                  Text(
                    text = if (conn.isConnected) conn.handle else "Not connected",
                    color = if (conn.isConnected) CyanAccent else TextTertiary,
                    fontSize = 11.sp
                  )
                }
              }

              Switch(
                checked = conn.isConnected,
                onCheckedChange = {
                  StudioAccountManager.toggleSocialConnection(conn.platformName)
                  Toast.makeText(
                    context,
                    if (conn.isConnected) "Disconnected ${conn.platformName}" else "Connected ${conn.platformName}!",
                    Toast.LENGTH_SHORT
                  ).show()
                },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = Color.Black,
                  checkedTrackColor = CyanAccent
                )
              )
            }
          }
        }
      }
    }

    // Account Management & Utilities Options
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .border(1.dp, StudioBorder, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = StudioSurface)
      ) {
        Column(modifier = Modifier.padding(8.dp)) {
          AccountOptionRow(
            icon = Icons.Default.Feedback,
            title = "Feedback & Bug Report",
            subtitle = "Send thoughts & feature requests to developers",
            onClick = { showFeedbackDialog = true }
          )

          AccountOptionRow(
            icon = Icons.Default.CleaningServices,
            title = "Clear App Cache",
            subtitle = "Free up 48.5 MB temporary render buffers",
            onClick = {
              val cleared = StudioAccountManager.clearAppCache()
              Toast.makeText(context, "Cache cleared successfully! 48.5 MB freed.", Toast.LENGTH_SHORT).show()
            }
          )

          AccountOptionRow(
            icon = Icons.Default.Description,
            title = "Terms & Conditions",
            subtitle = "Studio usage policies & licensing",
            onClick = { showTermsDialog = true }
          )

          AccountOptionRow(
            icon = Icons.Default.PrivacyTip,
            title = "Privacy Policy",
            subtitle = "Data protection & privacy rights",
            onClick = { showPrivacyDialog = true }
          )

          AccountOptionRow(
            icon = Icons.Default.SwitchAccount,
            title = "Switch Account Profile",
            subtitle = "Currently active: ${profile.name}",
            onClick = { showSwitchAccountDialog = true }
          )

          AccountOptionRow(
            icon = Icons.Default.Logout,
            title = "Log Out",
            subtitle = "Sign out of active creator session",
            textColor = RoseAccent,
            onClick = { showLogOutDialog = true }
          )
        }
      }
    }

    // App Version Footer
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "AH Video Studio • Mobile Edition",
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextTertiary, fontSize = 11.sp)
        )
        Text(
          text = "v3.5.0 (Build 2026.09) • Professional Timeline Engine",
          style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontSize = 10.sp)
        )
      }
    }
  }

  // Edit Profile Dialog
  if (showEditProfileDialog) {
    var editName by remember { mutableStateOf(profile.name) }
    var editHandle by remember { mutableStateOf(profile.handle) }
    var editBio by remember { mutableStateOf(profile.bio) }
    var editEmail by remember { mutableStateOf(profile.email) }

    AlertDialog(
      onDismissRequest = { showEditProfileDialog = false },
      title = { Text("Edit Creator Profile", color = TextPrimary) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = editName,
            onValueChange = { editName = it },
            label = { Text("Display Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = editHandle,
            onValueChange = { editHandle = it },
            label = { Text("Handle (@username)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = editEmail,
            onValueChange = { editEmail = it },
            label = { Text("Email Address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = editBio,
            onValueChange = { editBio = it },
            label = { Text("Bio / Tagline") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            StudioAccountManager.updateProfile(editName, editHandle, editBio, editEmail)
            Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            showEditProfileDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
        ) {
          Text("Save Changes")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditProfileDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      },
      containerColor = StudioSurface
    )
  }

  // Feedback Dialog
  if (showFeedbackDialog) {
    var feedbackText by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showFeedbackDialog = false },
      title = { Text("Feedback & Bug Report", color = TextPrimary) },
      text = {
        Column {
          Text("Tell us what you'd like to see improved in AH Video Studio:", color = TextSecondary, fontSize = 12.sp)
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = feedbackText,
            onValueChange = { feedbackText = it },
            placeholder = { Text("Write your feedback or issue report here...") },
            modifier = Modifier
              .fillMaxWidth()
              .height(110.dp)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            Toast.makeText(context, "Thank you! Your feedback has been submitted.", Toast.LENGTH_SHORT).show()
            showFeedbackDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
        ) {
          Text("Submit")
        }
      },
      dismissButton = {
        TextButton(onClick = { showFeedbackDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      },
      containerColor = StudioSurface
    )
  }

  // Terms Dialog
  if (showTermsDialog) {
    AlertDialog(
      onDismissRequest = { showTermsDialog = false },
      title = { Text("Terms & Conditions", color = TextPrimary) },
      text = {
        Text(
          "AH Video Studio Terms of Service:\n\n1. All user projects and exported video clips remain 100% owned by the creator.\n2. Local auto-saving & timeline crash recovery protects your work on device.\n3. Pro features offer high frame rate and 4K export capabilities.",
          color = TextSecondary,
          fontSize = 12.sp
        )
      },
      confirmButton = {
        Button(
          onClick = { showTermsDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
        ) {
          Text("Close")
        }
      },
      containerColor = StudioSurface
    )
  }

  // Privacy Dialog
  if (showPrivacyDialog) {
    AlertDialog(
      onDismissRequest = { showPrivacyDialog = false },
      title = { Text("Privacy Policy", color = TextPrimary) },
      text = {
        Text(
          "Privacy Policy Summary:\n\nYour video files, raw media assets, and timeline projects are stored locally on your device. AH Video Studio respects your privacy and does not upload your raw media without explicit permission.",
          color = TextSecondary,
          fontSize = 12.sp
        )
      },
      confirmButton = {
        Button(
          onClick = { showPrivacyDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
        ) {
          Text("Close")
        }
      },
      containerColor = StudioSurface
    )
  }

  // Switch Account Dialog
  if (showSwitchAccountDialog) {
    AlertDialog(
      onDismissRequest = { showSwitchAccountDialog = false },
      title = { Text("Switch Creator Account", color = TextPrimary) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          userAccounts.forEach { acc ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (acc.isCurrent) StudioSurfaceVariant else Color.Transparent)
                .clickable {
                  StudioAccountManager.switchActiveAccount(acc.id)
                  Toast.makeText(context, "Switched to ${acc.name}", Toast.LENGTH_SHORT).show()
                  showSwitchAccountDialog = false
                }
                .padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(Color(acc.avatarColor)),
                contentAlignment = Alignment.Center
              ) {
                Text(acc.name.take(1), fontWeight = FontWeight.Bold, color = Color.Black)
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(acc.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                Text(acc.email, color = TextTertiary, fontSize = 11.sp)
              }
              if (acc.isCurrent) {
                Icon(Icons.Default.Check, contentDescription = null, tint = CyanAccent)
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showSwitchAccountDialog = false }) {
          Text("Close", color = CyanAccent)
        }
      },
      containerColor = StudioSurface
    )
  }

  // Log Out Dialog
  if (showLogOutDialog) {
    AlertDialog(
      onDismissRequest = { showLogOutDialog = false },
      title = { Text("Log Out?", color = TextPrimary) },
      text = { Text("Sign out of active creator session? Your local project drafts will remain safe on this device.", color = TextSecondary) },
      confirmButton = {
        Button(
          onClick = {
            showLogOutDialog = false
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = RoseAccent, contentColor = Color.White)
        ) {
          Text("Log Out")
        }
      },
      dismissButton = {
        TextButton(onClick = { showLogOutDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      },
      containerColor = StudioSurface
    )
  }
}

// Helper Composable Components
@Composable
private fun AccountStatItem(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = value, fontWeight = FontWeight.Bold, color = CyanAccent, fontSize = 15.sp)
    Text(text = label, color = TextTertiary, fontSize = 11.sp)
  }
}

@Composable
private fun AccountOptionRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  textColor: Color = TextPrimary,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(imageVector = icon, contentDescription = title, tint = if (textColor == RoseAccent) RoseAccent else CyanAccent, modifier = Modifier.size(22.dp))
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
      Text(text = subtitle, color = TextTertiary, fontSize = 11.sp)
    }
    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
  }
}

// Modal dialog to create custom template
@Composable
private fun CreateTemplateModalDialog(
  onDismiss: () -> Unit,
  onCreate: (title: String, category: String, description: String, durationSec: Int, aspect: AspectRatio) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("User-Created") }
  var description by remember { mutableStateOf("") }
  var durationSec by remember { mutableStateOf("6") }
  var selectedAspect by remember { mutableStateOf(AspectRatio.RATIO_9_16) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Create Custom Template", color = TextPrimary) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Template Title") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = category,
          onValueChange = { category = it },
          label = { Text("Category (e.g. Reels, Vlog, Ads)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = durationSec,
          onValueChange = { durationSec = it.filter { char -> char.isDigit() } },
          label = { Text("Duration (Seconds)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description & Style Notes") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank()) {
            onCreate(title, category, description, durationSec.toIntOrNull() ?: 6, selectedAspect)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
      ) {
        Text("Save Template")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    },
    containerColor = StudioSurface
  )
}

// Modal dialog to preview template details
@Composable
private fun TemplatePreviewModalDialog(
  template: VideoTemplate,
  isSaved: Boolean,
  onToggleSave: () -> Unit,
  onUseTemplate: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("${template.iconEmoji} ${template.title}", color = TextPrimary) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Category: ${template.category}", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(template.description, color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text("🎵 Soundtrack: ${template.audioTitle}", color = TextPrimary, fontSize = 12.sp)
        Text("⏱️ Duration: ${template.durationMs / 1000}s • Aspect: ${template.aspectRatio.label}", color = TextTertiary, fontSize = 11.sp)
        Text("🎞️ Media Slots: ${template.mediaPlaceholders.size} • Text Slots: ${template.textPlaceholders.size}", color = TextTertiary, fontSize = 11.sp)
      }
    },
    confirmButton = {
      Button(
        onClick = onUseTemplate,
        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
      ) {
        Text("Use Template")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close", color = TextSecondary)
      }
    },
    containerColor = StudioSurface
  )
}
