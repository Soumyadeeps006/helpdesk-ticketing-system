package com.helpdesk.dao;

import com.helpdesk.model.User;
import java.util.List;
import java.util.Optional;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data Access Object for {@link User} entities.
 * Path: src/main/java/com/helpdesk/dao/UserDAO.java
 *
 * Uses Hibernate SessionFactory injected via Spring.
 * All write operations require an active transaction (@Transactional on caller).
 */
@Repository
public class UserDAO {

    private final SessionFactory sessionFactory;

    @Autowired
    public UserDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    @Transactional
    public User save(User user) {
        sessionFactory.getCurrentSession().saveOrUpdate(user);
        return user;
    }

    @Transactional
    public void delete(Long userId) {
        User user = findById(userId).orElseThrow(() ->
            new IllegalArgumentException("User not found: " + userId)
        );
        sessionFactory.getCurrentSession().delete(user);
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        User user = sessionFactory.getCurrentSession().get(User.class, id);
        return Optional.ofNullable(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return sessionFactory
            .getCurrentSession()
            .createQuery("FROM User u WHERE u.username = :username", User.class)
            .setParameter("username", username)
            .uniqueResultOptional();
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return sessionFactory
            .getCurrentSession()
            .createQuery("FROM User u WHERE u.email = :email", User.class)
            .setParameter("email", email)
            .uniqueResultOptional();
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return sessionFactory
            .getCurrentSession()
            .createQuery("FROM User u ORDER BY u.username ASC", User.class)
            .list();
    }

    @Transactional(readOnly = true)
    public List<User> findByRole(String role) {
        return sessionFactory
            .getCurrentSession()
            .createQuery(
                "FROM User u WHERE u.role = :role ORDER BY u.fullName ASC",
                User.class
            )
            .setParameter("role", role)
            .list();
    }

    @Transactional(readOnly = true)
    public List<User> findAllItStaff() {
        return findByRole("IT_STAFF");
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        Long count = sessionFactory
            .getCurrentSession()
            .createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.username = :username",
                Long.class
            )
            .setParameter("username", username)
            .uniqueResult();
        return count != null && count > 0;
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        Long count = sessionFactory
            .getCurrentSession()
            .createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.email = :email",
                Long.class
            )
            .setParameter("email", email)
            .uniqueResult();
        return count != null && count > 0;
    }
}
