package com.projectronin.interop.validation.server.controller

import com.projectronin.interop.validation.server.data.IssueDAO
import com.projectronin.interop.validation.server.data.model.IssueDO
import com.projectronin.interop.validation.server.generated.apis.IssueApi
import com.projectronin.interop.validation.server.generated.models.Issue
import com.projectronin.interop.validation.server.generated.models.IssueStatus
import com.projectronin.interop.validation.server.generated.models.Order
import com.projectronin.interop.validation.server.generated.models.UpdateIssue
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class IssueController(private val issueDAO: IssueDAO) : IssueApi {
    val logger = KotlinLogging.logger { }

    @PreAuthorize("hasAuthority('SCOPE_read:issues')")
    override fun getIssues(
        resourceId: UUID,
        status: List<IssueStatus>?,
        order: Order,
        limit: Int,
        after: UUID?
    ): ResponseEntity<List<Issue>> {
        val statuses = if (status == null || status.isEmpty()) IssueStatus.values().toList() else status

        val issueDOs = issueDAO.getIssues(resourceId, statuses, order, limit, after)
        if (issueDOs.isEmpty()) {
            return ResponseEntity.ok(listOf())
        }

        val issues = issueDOs.map {
            createIssue(it)
        }

        return ResponseEntity.ok(issues)
    }

    @PreAuthorize("hasAuthority('SCOPE_read:issues')")
    override fun getIssueById(resourceId: UUID, issueId: UUID): ResponseEntity<Issue> {
        val issueDO = issueDAO.getIssue(issueId) ?: return ResponseEntity.notFound().build()
        if (issueDO.resourceId != resourceId) {
            logger.info {
                "Mismatch between resource ID [${issueDO.resourceId}] on issue found [${issueDO.id}] " +
                    "and resource ID [$resourceId] requested"
            }
            return ResponseEntity.badRequest().build()
        }
        val issue = createIssue(issueDO)
        return ResponseEntity.ok(issue)
    }

    @PreAuthorize("hasAuthority('SCOPE_update:issues')")
    override fun updateIssue(resourceId: UUID, issueId: UUID, updateIssue: UpdateIssue): ResponseEntity<Issue> {
        val updatedIssueDO = issueDAO.updateIssue(
            resourceId,
            issueId,
            IssueDO {
                severity = updateIssue.severity
                type = updateIssue.type
                location = updateIssue.location
                description = updateIssue.description
                status = updateIssue.status
                updateDateTime = updateIssue.updateDtTm
            }
        ) ?: return ResponseEntity.notFound().build()
        val issue = createIssue(updatedIssueDO)
        return ResponseEntity.ok(issue)
    }

    private fun createIssue(issueDO: IssueDO): Issue {
        return Issue(
            id = issueDO.id,
            severity = issueDO.severity,
            type = issueDO.type,
            location = issueDO.location,
            description = issueDO.description,
            status = issueDO.status,
            createDtTm = issueDO.createDateTime,
            updateDtTm = issueDO.updateDateTime,
        )
    }
}
