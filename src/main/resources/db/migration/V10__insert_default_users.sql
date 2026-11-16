/*
 * Version reservada del esquema. En una instalacion nueva no se crean
 * sucursales, roles ni usuarios de demostracion mediante Flyway.
 *
 * El primer administrador y su sucursal se crean explicitamente mediante
 * InitialAdminBootstrap. Esta version solo es valida para bases nuevas;
 * las bases con la V10 anterior requieren el procedimiento documentado.
 */
SELECT 1;