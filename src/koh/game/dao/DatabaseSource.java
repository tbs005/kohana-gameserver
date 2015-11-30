package koh.game.dao;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import koh.patterns.services.api.DependsOn;
import koh.patterns.services.api.Service;
import koh.game.app.Loggers;
import koh.game.app.MemoryService;
import koh.game.utils.Settings;
import koh.game.utils.sql.ConnectionResult;
import koh.game.utils.sql.ConnectionStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author Neo-Craft
 */

@DependsOn({Loggers.class, MemoryService.class})
public class DatabaseSource implements Service {

    @Override
    public void inject(Injector injector){
    }

    @Override
    public void configure(Binder binder){
        binder.requestStaticInjection(DAO.class);
    }

    @Inject private Settings settings;

    private HikariDataSource dataSource;

    @Override
    public void start() {
        if(dataSource != null && !dataSource.isClosed())
            dataSource.close();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + settings.getStringElement("Database.Host") + "/" + settings.getStringElement("Database.name"));
        config.setUsername(settings.getStringElement("Database.User"));
        config.setPassword(settings.getStringElement("Database.Password"));
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setMaximumPoolSize(50);

        this.dataSource = new HikariDataSource(config);

    }

    @Override
    public void stop() {

        if(dataSource != null)
            dataSource.close();
    }

    public Connection getConnectionOfPool() throws SQLException {
        return dataSource.getConnection();
    }

    public ConnectionStatement<Statement> createStatement() throws SQLException {
        Connection connection = this.getConnectionOfPool();
        return new ConnectionStatement<>(connection, connection.createStatement());
    }

    public ConnectionStatement<PreparedStatement> prepareStatement(String query) throws SQLException {
        Connection connection = this.getConnectionOfPool();
        return new ConnectionStatement<>(connection, connection.prepareStatement(query));
    }

    public ConnectionStatement<PreparedStatement> prepareStatement(String query, boolean autoGeneratedKeys) throws SQLException {
        Connection connection = this.getConnectionOfPool();
        PreparedStatement statement = connection.prepareStatement(query,
                autoGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
        return new ConnectionStatement<>(connection, statement);
    }

    public ConnectionResult executeQuery(String query) throws SQLException {
        Connection connection = this.getConnectionOfPool();
        Statement statement = connection.createStatement();
        statement.setQueryTimeout(300);
        return new ConnectionResult(connection, statement, statement.executeQuery(query));
    }

    public ConnectionResult executeQuery(String query, int secsTimeout) throws SQLException {
        Connection connection = this.getConnectionOfPool();
        Statement statement = connection.createStatement();
        if(secsTimeout > 0)
            statement.setQueryTimeout(secsTimeout);
        return new ConnectionResult(connection, statement, statement.executeQuery(query));
    }

}
