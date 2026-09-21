package lithan.autostrada.auctions.migration;

/** Offline Flyway entry point for the controlled maintenance container. */
public final class SchemaMaintenance {
  public static void main(String[] args) {
    try {
      var config=org.flywaydb.core.Flyway.configure().dataSource(System.getenv("DB_URL"),System.getenv("DB_USERNAME"),System.getenv("DB_PASSWORD"));
      String target=System.getenv("MIGRATION_TARGET");if(target!=null&&!target.isBlank())config.target(target);
      config.load().migrate();
    } catch(Exception error) {
      System.err.println("Schema migration failed; keep writers stopped. Inspect private migration diagnostics.");System.exit(1);
    }
  }
}
