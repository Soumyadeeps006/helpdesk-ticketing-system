<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>New Ticket — IT Help Desk</title>
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
    <div class="row justify-content-center">
        <div class="col-lg-7">

            <%-- Flash error --%>
            <c:if test="${not empty errorMessage}">
                <div class="alert alert-danger alert-dismissible fade show">
                    ${errorMessage}
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </c:if>

            <div class="card shadow-sm">
                <div class="card-header">
                    <h5 class="mb-0">Create New Ticket</h5>
                </div>
                <div class="card-body">
                    <form method="post" action="${pageContext.request.contextPath}/tickets" novalidate>

                        <%-- Title --%>
                        <div class="mb-3">
                            <label for="title" class="form-label fw-semibold">Title <span class="text-danger">*</span></label>
                            <input type="text" id="title" name="title"
                                   class="form-control ${not empty fieldErrors.title ? 'is-invalid' : ''}"
                                   value="${formData.title}" placeholder="Brief summary of the issue" required>
                            <c:if test="${not empty fieldErrors.title}">
                                <div class="invalid-feedback">${fieldErrors.title}</div>
                            </c:if>
                        </div>

                        <%-- Description --%>
                        <div class="mb-3">
                            <label for="description" class="form-label fw-semibold">Description <span class="text-danger">*</span></label>
                            <textarea id="description" name="description"
                                      class="form-control ${not empty fieldErrors.description ? 'is-invalid' : ''}"
                                      rows="5" placeholder="Describe the issue in detail..." required>${formData.description}</textarea>
                            <c:if test="${not empty fieldErrors.description}">
                                <div class="invalid-feedback">${fieldErrors.description}</div>
                            </c:if>
                        </div>

                        <%-- Priority --%>
                        <div class="mb-3">
                            <label for="priority" class="form-label fw-semibold">Priority <span class="text-danger">*</span></label>
                            <select id="priority" name="priority" class="form-select" required>
                                <option value="" disabled ${empty formData.priority ? 'selected' : ''}>— Select priority —</option>
                                <c:forEach var="p" items="${priorities}">
                                    <option value="${p}" ${formData.priority == p ? 'selected' : ''}>${p}</option>
                                </c:forEach>
                            </select>
                            <div class="form-text">
                                <span class="badge priority-badge priority-LOW me-1">LOW</span>
                                <span class="badge priority-badge priority-MEDIUM me-1">MEDIUM</span>
                                <span class="badge priority-badge priority-HIGH me-1">HIGH</span>
                                <span class="badge priority-badge priority-CRITICAL">CRITICAL</span>
                            </div>
                        </div>

                        <%-- Assign To (optional) --%>
                        <div class="mb-4">
                            <label for="assignedToId" class="form-label fw-semibold">Assign To</label>
                            <select id="assignedToId" name="assignedToId" class="form-select">
                                <option value="">— Unassigned —</option>
                                <c:forEach var="user" items="${users}">
                                    <option value="${user.id}" ${formData.assignedToId == user.id ? 'selected' : ''}>${user.username}</option>
                                </c:forEach>
                            </select>
                        </div>

                        <div class="d-flex gap-2">
                            <button type="submit" class="btn btn-primary">Create Ticket</button>
                            <a href="${pageContext.request.contextPath}/tickets" class="btn btn-outline-secondary">Cancel</a>
                        </div>
                    </form>
                </div>
            </div>

        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
