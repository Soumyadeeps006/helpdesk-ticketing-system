<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Ticket #${ticket.id} — IT Help Desk</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="bg-light">

<nav class="navbar navbar-dark bg-primary mb-4">
    <div class="container">
        <a class="navbar-brand fw-bold" href="${pageContext.request.contextPath}/tickets">🎫 IT Help Desk</a>
        <a href="${pageContext.request.contextPath}/tickets" class="btn btn-outline-light btn-sm">← Back to Tickets</a>
    </div>
</nav>

<div class="container">

    <%-- Flash messages --%>
    <c:if test="${not empty successMessage}">
        <div class="alert alert-success alert-dismissible fade show">
            ${successMessage}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger alert-dismissible fade show">
            ${errorMessage}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>

    <div class="row g-4">

        <%-- Ticket Detail Card --%>
        <div class="col-lg-8">
            <div class="card shadow-sm mb-4">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <h5 class="mb-0">Ticket #${ticket.id}</h5>
                    <span class="badge status-badge status-${ticket.status} fs-6">${ticket.status}</span>
                </div>
                <div class="card-body">
                    <h4>${ticket.title}</h4>
                    <p class="text-muted">${ticket.description}</p>
                    <hr>
                    <div class="row text-sm">
                        <div class="col-sm-6 mb-2">
                            <strong>Priority:</strong>
                            <span class="badge priority-badge priority-${ticket.priority} ms-1">${ticket.priority}</span>
                        </div>
                        <div class="col-sm-6 mb-2">
                            <strong>Created:</strong>
                            <span class="text-muted ms-1">
                                <fmt:formatDate value="${ticket.createdAt}" pattern="MMM dd, yyyy HH:mm"/>
                            </span>
                        </div>
                        <div class="col-sm-6 mb-2">
                            <strong>Reported By:</strong>
                            <span class="text-muted ms-1">
                                <c:out value="${ticket.createdBy.username}" default="Unknown"/>
                            </span>
                        </div>
                        <div class="col-sm-6 mb-2">
                            <strong>Assigned To:</strong>
                            <span class="text-muted ms-1">
                                <c:choose>
                                    <c:when test="${not empty ticket.assignedTo}">${ticket.assignedTo.username}</c:when>
                                    <c:otherwise>Unassigned</c:otherwise>
                                </c:choose>
                            </span>
                        </div>
                    </div>
                </div>

                <%-- Action Buttons --%>
                <div class="card-footer bg-transparent">
                    <strong class="d-block mb-2 small text-muted">Actions</strong>
                    <div class="d-flex flex-wrap gap-2">

                        <%-- Start Progress --%>
                        <c:if test="${ticket.status == 'OPEN'}">
                            <form method="post" action="${pageContext.request.contextPath}/tickets/${ticket.id}/status">
                                <input type="hidden" name="status" value="IN_PROGRESS">
                                <button type="submit" class="btn btn-warning btn-sm">▶ Start Progress</button>
                            </form>
                        </c:if>

                        <%-- Close Ticket --%>
                        <c:if test="${ticket.status != 'CLOSED'}">
                            <form method="post" action="${pageContext.request.contextPath}/tickets/${ticket.id}/status">
                                <input type="hidden" name="status" value="CLOSED">
                                <button type="submit" class="btn btn-danger btn-sm">✔ Close Ticket</button>
                            </form>
                        </c:if>

                        <%-- Reopen --%>
                        <c:if test="${ticket.status == 'CLOSED'}">
                            <form method="post" action="${pageContext.request.contextPath}/tickets/${ticket.id}/status">
                                <input type="hidden" name="status" value="OPEN">
                                <button type="submit" class="btn btn-success btn-sm">↩ Reopen</button>
                            </form>
                        </c:if>

                        <%-- Change Priority --%>
                        <form method="post" action="${pageContext.request.contextPath}/tickets/${ticket.id}/priority" class="d-flex gap-1">
                            <select name="priority" class="form-select form-select-sm" style="width:auto;">
                                <c:forEach var="p" items="${priorities}">
                                    <option value="${p}" ${ticket.priority == p ? 'selected' : ''}>${p}</option>
                                </c:forEach>
                            </select>
                            <button type="submit" class="btn btn-outline-secondary btn-sm">Update Priority</button>
                        </form>

                        <%-- View Audit Timeline --%>
                        <a href="${pageContext.request.contextPath}/tickets/${ticket.id}/timeline" class="btn btn-outline-info btn-sm">📋 Timeline</a>
                    </div>
                </div>
            </div>

            <%-- Comments Section --%>
            <div class="card shadow-sm">
                <div class="card-header">
                    <h6 class="mb-0">💬 Comments</h6>
                </div>
                <div class="card-body">
                    <c:choose>
                        <c:when test="${empty comments}">
                            <p class="text-muted small">No comments yet. Be the first!</p>
                        </c:when>
                        <c:otherwise>
                            <%-- Render comment tree (flat list with depth indentation) --%>
                            <c:forEach var="comment" items="${comments}">
                                <div class="comment-block" style="margin-left: ${comment.depth * 24}px;">
                                    <div class="card mb-2 comment-card ${comment.depth > 0 ? 'reply-card' : ''}">
                                        <div class="card-body py-2 px-3">
                                            <div class="d-flex justify-content-between mb-1">
                                                <strong class="small">${comment.author.username}</strong>
                                                <span class="text-muted small">
                                                    <fmt:formatDate value="${comment.createdAt}" pattern="MMM dd, yyyy HH:mm"/>
                                                </span>
                                            </div>
                                            <p class="mb-1 small">${comment.content}</p>
                                            <c:if test="${ticket.status != 'CLOSED'}">
                                                <button class="btn btn-link btn-sm p-0 text-muted reply-toggle"
                                                        data-comment-id="${comment.id}">↩ Reply</button>
                                                <div class="reply-form mt-2 d-none" id="reply-${comment.id}">
                                                    <form method="post" action="${pageContext.request.contextPath}/comments/reply">
                                                        <input type="hidden" name="ticketId" value="${ticket.id}">
                                                        <input type="hidden" name="parentId" value="${comment.id}">
                                                        <textarea name="content" class="form-control form-control-sm mb-1" rows="2" placeholder="Write a reply..." required></textarea>
                                                        <button type="submit" class="btn btn-primary btn-sm">Post Reply</button>
                                                    </form>
                                                </div>
                                            </c:if>
                                        </div>
                                    </div>
                                </div>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>

                <%-- Add Root Comment --%>
                <c:if test="${ticket.status != 'CLOSED'}">
                    <div class="card-footer bg-transparent">
                        <form method="post" action="${pageContext.request.contextPath}/comments">
                            <input type="hidden" name="ticketId" value="${ticket.id}">
                            <textarea name="content" class="form-control mb-2" rows="3" placeholder="Add a comment..." required></textarea>
                            <button type="submit" class="btn btn-primary btn-sm">Post Comment</button>
                        </form>
                    </div>
                </c:if>
            </div>
        </div>

        <%-- Sidebar: Quick Info --%>
        <div class="col-lg-4">
            <div class="card shadow-sm">
                <div class="card-header"><h6 class="mb-0">Ticket Info</h6></div>
                <ul class="list-group list-group-flush">
                    <li class="list-group-item d-flex justify-content-between">
                        <span class="text-muted small">Status</span>
                        <span class="badge status-badge status-${ticket.status}">${ticket.status}</span>
                    </li>
                    <li class="list-group-item d-flex justify-content-between">
                        <span class="text-muted small">Priority</span>
                        <span class="badge priority-badge priority-${ticket.priority}">${ticket.priority}</span>
                    </li>
                    <li class="list-group-item d-flex justify-content-between">
                        <span class="text-muted small">Comments</span>
                        <span class="fw-semibold">${comments.size()}</span>
                    </li>
                    <li class="list-group-item d-flex justify-content-between">
                        <span class="text-muted small">Ticket ID</span>
                        <span class="fw-semibold">#${ticket.id}</span>
                    </li>
                    <li class="list-group-item">
                        <a href="${pageContext.request.contextPath}/tickets/${ticket.id}/timeline"
                           class="btn btn-outline-secondary btn-sm w-100">View Audit Timeline</a>
                    </li>
                </ul>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>
