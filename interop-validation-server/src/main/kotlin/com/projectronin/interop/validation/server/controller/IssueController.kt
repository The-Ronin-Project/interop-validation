package com.projectronin.interop.validation.server.controller

import com.projectronin.interop.validation.server.apis.IssueApi
import com.projectronin.interop.validation.server.models.Issue
import com.projectronin.interop.validation.server.models.IssueStatus
import com.projectronin.interop.validation.server.models.UpdateIssue
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class IssueController : IssueApi {
    override fun getIssues(
        resourceId: UUID,
        status: List<IssueStatus>?,
        order: String,
        limit: Int,
        after: UUID?
    ): ResponseEntity<List<Issue>> {
        TODO()
    }

    override fun getIssueById(resourceId: UUID, issueId: UUID): ResponseEntity<Issue> {
        TODO()
    }

    override fun updateIssue(resourceId: UUID, issueId: UUID, updateIssue: UpdateIssue): ResponseEntity<Issue> {
        TODO()
    }
}
