package com.company.hrsystem.attendance.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostgresDistributedLockManager {

    private static final String TRY_LOCK_SQL = "select pg_try_advisory_lock(?)";
    private static final String UNLOCK_SQL = "select pg_advisory_unlock(?)";

    private final DataSource dataSource;

    public Optional<LockHandle> tryAcquire(long lockKey) {
        try {
            var connection = dataSource.getConnection();
            var acquired = tryLock(connection, lockKey);
            if (!acquired) {
                connection.close();
                return Optional.empty();
            }
            return Optional.of(new AdvisoryLockHandle(connection, lockKey));
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to acquire distributed DB lock", ex);
        }
    }

    private boolean tryLock(Connection connection, long lockKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(TRY_LOCK_SQL)) {
            statement.setLong(1, lockKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean(1);
            }
        }
    }

    public interface LockHandle extends AutoCloseable {

        @Override
        void close();
    }

    private static final class AdvisoryLockHandle implements LockHandle {

        private final Connection connection;
        private final long lockKey;
        private final AtomicBoolean released = new AtomicBoolean(false);

        private AdvisoryLockHandle(Connection connection, long lockKey) {
            this.connection = connection;
            this.lockKey = lockKey;
        }

        @Override
        public void close() {
            if (!released.compareAndSet(false, true)) {
                return;
            }

            try (PreparedStatement unlockStatement = connection.prepareStatement(UNLOCK_SQL)) {
                unlockStatement.setLong(1, lockKey);
                try (ResultSet ignored = unlockStatement.executeQuery()) {
                    // no-op
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("Unable to release distributed DB lock", ex);
            } finally {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Unable to close DB lock connection", ex);
                }
            }
        }
    }
}
