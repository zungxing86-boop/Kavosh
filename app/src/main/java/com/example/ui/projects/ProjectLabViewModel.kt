package com.example.ui.projects

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProjectLabViewModel(private val container: AppContainer) : ViewModel() {

    val projects: StateFlow<List<Project>> = container.journeyRepository.projectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun observeAttachments(projectId: Long): Flow<List<ProjectAttachment>> {
        return container.journeyRepository.observeProjectAttachments(projectId)
    }

    suspend fun saveProject(project: Project, isPersian: Boolean): Long {
        val id = container.journeyRepository.createOrUpdateProject(project)
        val isCompleted = project.status == ProjectStatus.COMPLETED
        if (isCompleted) {
            container.journeyRepository.recordSkillExperience(
                skillIds = listOf("skill_creativity", "skill_planning", "skill_responsibility"),
                pointsPerSkill = 2,
                experienceLabel = project.title
            )
        }
        val allAchievements = container.contentRepository.getAchievements()
        val unlockedIds = container.journeyRepository.userAchievementsFlow.first().map { it.achievementId }.toSet()
        val missions = container.contentRepository.getMissions()
        val progress = container.journeyRepository.missionProgressFlow.first()
        val allProjects = container.journeyRepository.projectsFlow.first()
        val skills = container.journeyRepository.skillProgressFlow.first()
        val journalCount = container.journeyRepository.journalEntriesFlow.first().size
        val newUnlocks = container.achievementEngine.evaluateNewUnlocks(
            allAchievements = allAchievements,
            unlockedIds = unlockedIds,
            missions = missions,
            progressList = progress,
            projects = allProjects,
            skillProgressList = skills,
            journalCount = journalCount
        )
        if (newUnlocks.isNotEmpty()) {
            container.journeyRepository.unlockAchievements(newUnlocks)
        }
        return id
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            container.journeyRepository.deleteProject(projectId)
        }
    }

    fun attachFileFromUri(projectId: Long, uri: Uri, isPersian: Boolean) {
        viewModelScope.launch {
            val result = container.fileStorage.copyUriToAttachments(uri)
            result.onSuccess { copied ->
                container.journeyRepository.addProjectAttachment(
                    ProjectAttachment(
                        projectId = projectId,
                        fileName = copied.fileName,
                        mimeType = copied.mimeType,
                        internalPath = copied.internalPath,
                        fileSizeBytes = copied.sizeBytes
                    )
                )
                _message.value = if (isPersian) "فایل با موفقیت اضافه شد" else "Attachment added safely"
            }.onFailure {
                _message.value = if (isPersian) "خطا در کپی فایل انتخاب‌شده" else "Could not import selected file"
            }
        }
    }

    fun removeAttachment(attachmentId: Long) {
        viewModelScope.launch {
            container.journeyRepository.deleteProjectAttachment(attachmentId)
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProjectLabViewModel(container) as T
        }
    }
}
