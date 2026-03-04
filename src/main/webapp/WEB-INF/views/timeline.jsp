<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Audit Timeline — Ticket #${ticket.id}</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="bg-light">

<nav class="navbar navbar-dark bg-primary mb-4">
    <div class="container">
        <a class="navbar-brand fw-bold" href="${pageContext.request.contextPath}/tickets">🎫 IT Help Desk</a>
        <a href="${pageContext.request.contextPath}/tickets/${ticket.id}" class="btn btn-outline-light btn-sm">← Back to Ticket</a>
    </div>
</nav>

<div class="container">
    <div class="row justify-content-center">
        <div class="col-lg-8">

            <div class="card shadow-sm mb-4">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <h5 class="mb-0">📋 Audit Timeline</h5>
                    <span class="text-muted small">Ticket #${ticket.id} — ${ticket.title}</span>
                </div>
                <div class="card-body">
                    <c:choose>
                        <c:when test="${empty auditLogs}">
                            <p class="text-muted text-center py-4">No audit events recorded yet.</p>
                        </c:when>
                        <c:otherwise>
                            <div class="timeline">
                                <c:forEach var="log" items="${auditLogs}" varStatus="status">
                                    <div class="timeline-item ${status.last ? 'timeline-item--last' : ''}">
                                        <div class="timeline-dot timeline-dot--${log.action}"></div>
                                        <div class="timeline-content card">
                                            <div class="card-body py-2 px-3">
                                                <div class="d-flex justify-content-between align-items-start">
                                                    <div>
                                                        <span class="badge audit-action-badge me-2">${log.action}</span>
                                                        <span class="small fw-semibold">${log.actor.username}</span>
                                                        <p class="mb-0 small text-muted mt-1">${log.description}</p>
                                                    </div>
                                                    <div class="text-end text-muted small ms-3 text-nowrap">
                                                        <fmt:formatDate value="${log.createdAt}" pattern="MMM dd, yyyy"/><br>
                                                        <fmt:formatDate value="${log.createdAt}" pattern="HH:mm:ss"/>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </c:forEach>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>

        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
