package com.frank.franchises.infrastructure.adapter.out.mongodb;

import com.mongodb.ConnectionString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de contrato con {@code terraform/outputs.tf} (output {@code mongodb_connection_uri}).
 * <p>
 * Verifica que las credenciales codificadas mediante la expresión de Terraform
 * {@code replace(urlencode(var.db_username), "+", "%20")} y
 * {@code replace(urlencode(var.db_password), "+", "%20")} son parseadas y
 * decodificadas sin alteración (incluyendo reservadas RFC 3986 y espacios) por
 * {@link ConnectionString} del driver oficial de MongoDB usado en Spring Boot.
 * </p>
 */
class MongoConnectionStringUriContractTest {

    @Test
    @DisplayName("Contrato con outputs.tf: ConnectionString round-trip con replace(urlencode(cred), '+', '%20') decodifica exactamente usuario y contraseña con caracteres reservados y espacio")
    void mongoConnectionStringRoundTrip_WithUrlEncodedSpecialCharsAndSpace_ShouldDecodeExactOriginalCredentials() {
        String rawUser = "user@admin:1";
        String rawPassword = "p@ss:w/rd?#%+ x";
        String terraformEncodedUser = "user%40admin%3A1";
        String terraformEncodedPassword = "p%40ss%3Aw%2Frd%3F%23%25%2B%20x";

        String uri = "mongodb://" + terraformEncodedUser + ":" + terraformEncodedPassword + "@localhost:27017/franchises?retryWrites=true&w=majority";
        ConnectionString connectionString = new ConnectionString(uri);

        assertThat(connectionString.getUsername()).isEqualTo(rawUser);
        assertThat(connectionString.getPassword()).isNotNull();
        assertThat(new String(connectionString.getPassword())).isEqualTo(rawPassword);
    }
}
