<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>IT Help Desk — Tickets</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="bg-light">

<nav class="navbar navbar-dark bg-primary mb-4">
    <div class="container">
        <a class="navbar-brand fw-bold" href="${pageContext.request.contextPath}/tickets">🎫 IT Help Desk</a>
        <a href="${pageContext.request.contextPath}/tickets/new" class="btn btn-light btn-sm">+ New Ticket</a>
    </div>
</nav>

<div class="container">

    <%-- Flash messages --%>
    <c:if test="${not empty successMessage}">
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            ${successMessage}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            ${errorMessage}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>

    <div class="card shadow-sm">
        <div class="card-header d-flex justify-content-between align-items-center">
            <h5 class="mb-0">All Tickets</h5>
            <span class="badge bg-secondary">${tickets.size()} total</span>
        </div>
        <div class="card-body p-0">
            <c:choose>
                <c:when test="${empty tickets}">
                    <p class="text-muted text-center p-4">No tickets found. <a href="${pageContext.request.contextPath}/tickets/new">Create one?</a></p>
                </c:when>
                <c:otherwise>
                    <table class="table table-hover mb-0">
                        <thead class="table-dark">
                            <tr>
                                <th>#</th>
                                <th>Title</th>
                                <th>Status</th>
                                <th>Priority</th>
                                <th>Created</th>
                                <th>Assigned To</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="ticket" items="${tickets}">
                                <tr>
                                    <td class="text-muted small">${ticket.id}</td>
                                    <td>
                                        <a href="${pageContext.request.contextPath}/tickets/${ticket.id}" class="text-decoration-none fw-semibold">
                                            ${ticket.title}
                                        </a>
                                    </td>
                                    <td>
                                        <span class="badge status-badge status-${ticket.status}">
                                            ${ticket.status}
                                        </span>
                                    </td>
                                    <td>
                                        <span class="badge priority-badge priority-${ticket.priority}">
                                            ${ticket.priority}
                                        </span>
                                    </td>
                                    <td class="small text-muted">
                                        <fmt:formatDate value="${ticket.createdAt}" pattern="MMM dd, yyyy HH:mm"/>
                                    </td>
                                    <td class="small">
                                        <c:choose>
                                            <c:when test="${not empty ticket.assignedTo}">
                                                ${ticket.assignedTo.username}
                                            </c:when>
                                            <c:otherwise>
                                                <span class="text-muted">Unassigned</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <a href="${pageContext.request.contextPath}/tickets/${ticket.id}" class="btn btn-outline-primary btn-sm">View</a>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
<div class="d-flex justify-content-between align-items-center mt-3">
    <div>
        Showing ${pageSize} of ${totalCount} tickets
    </div>
    <nav aria-label="Page navigation">
        <ul class="pagination mb-0">
            <li class="page-item ${currentPage == 0 ? 'disabled' : ''}">
                <a class="page-link" href="?page=${currentPage-1}&size=${pageSize}">Previous</a>
            </li>
            <c:forEach var="i" begin="0" end="${totalPages-1}">
                <li class="page-item ${i == currentPage ? 'active' : ''}">
                    <a class="page-link" href="?page=${i}&size=${pageSize}">${i+1}</a>
                </li>
            </c:forEach>
            <li class="page-item ${currentPage+1 >= totalPages ? 'disabled' : ''}">
                <a class="page-link" href="?page=${currentPage+1}&size=${pageSize}">Next</a>
            </li>
        </ul>
    </nav>
</div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
