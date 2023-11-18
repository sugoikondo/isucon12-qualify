package isucon12

import org.springframework.dao.DuplicateKeyException
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import kotlin.io.path.Path

@RestController
class AdminTenantAddController(
    private val jdbcTemplate: JdbcTemplate,
) {
    companion object {
        const val TENANT_DB_SCHEMA_FILE_PATH = "../sql/tenant/10_schema.sql"
    }

    @PostMapping("/api/admin/tenant/add")
    fun add(
        @RequestParam(name = "name") name: String,
        @RequestParam(name = "display_name") displayName: String
    ): ResponseEntity<*> {
        // confirm cookie
//        val viewer = TODO()

        if (validateTenantName(name)) {
            return ResponseEntity.badRequest().body("invalid tenant name")
        }

        val now = Instant.now().epochSecond
        try {
            jdbcTemplate.update(
                "insert into tenant (name, display_name, created_at, updated_at) values (?, ?, ?, ?)",
                name, displayName, now, now,
            )
        } catch (e: DuplicateKeyException) {
            return ResponseEntity.badRequest().body("duplicate tenant")
        }

        val id = jdbcTemplate.query("select id from tenant where name = ?", name) { rs, _ -> rs.getLong("id") }.first()
        createTenantDB(id)

        return ResponseEntity.ok().body(mapOf(
            "id" to id,
            "name" to name,
            "display_name" to displayName,
            "billing_yen" to 0,
        ))
    }

    private fun validateTenantName(name: String) = Regex("^[a-z][a-z0-9-]{0,61}[a-z0-9]$").matches(name)

    private fun createTenantDB(id: Long) {
        val tenantDbDir = System.getenv("ISUCON_TENANT_DB_DIR") ?: "../tenant_db"
        val tenantDbPath = Path(tenantDbDir).resolve("$id.db")

        // TODO: use SQLite driver
        ProcessBuilder("sh", "-c", "sqlite3 $tenantDbPath < $TENANT_DB_SCHEMA_FILE_PATH")
            .start()
            .waitFor()
    }
}

// func tenantsAddHandler(c echo.Context) error {
//	v, err := parseViewer(c)
//	if err != nil {
//		return fmt.Errorf("error parseViewer: %w", err)
//	}
//	if v.tenantName != "admin" {
//		// admin: SaaS管理者用の特別なテナント名
//		return echo.NewHTTPError(
//			http.StatusNotFound,
//			fmt.Sprintf("%s has not this API", v.tenantName),
//		)
//	}
//	if v.role != RoleAdmin {
//		return echo.NewHTTPError(http.StatusForbidden, "admin role required")
//	}
//
//	displayName := c.FormValue("display_name")
//	name := c.FormValue("name")
//	if err := validateTenantName(name); err != nil {
//		return echo.NewHTTPError(http.StatusBadRequest, err.Error())
//	}
//
//	ctx := context.Background()
//	now := time.Now().Unix()
//	insertRes, err := adminDB.ExecContext(
//		ctx,
//		"INSERT INTO tenant (name, display_name, created_at, updated_at) VALUES (?, ?, ?, ?)",
//		name, displayName, now, now,
//	)
//	if err != nil {
//		if merr, ok := err.(*mysql.MySQLError); ok && merr.Number == 1062 { // duplicate entry
//			return echo.NewHTTPError(http.StatusBadRequest, "duplicate tenant")
//		}
//		return fmt.Errorf(
//			"error Insert tenant: name=%s, displayName=%s, createdAt=%d, updatedAt=%d, %w",
//			name, displayName, now, now, err,
//		)
//	}
//
//	id, err := insertRes.LastInsertId()
//	if err != nil {
//		return fmt.Errorf("error get LastInsertId: %w", err)
//	}
//	// NOTE: 先にadminDBに書き込まれることでこのAPIの処理中に
//	//       /api/admin/tenants/billingにアクセスされるとエラーになりそう
//	//       ロックなどで対処したほうが良さそう
//	if err := createTenantDB(id); err != nil {
//		return fmt.Errorf("error createTenantDB: id=%d name=%s %w", id, name, err)
//	}
//
//	res := TenantsAddHandlerResult{
//		Tenant: TenantWithBilling{
//			ID:          strconv.FormatInt(id, 10),
//			Name:        name,
//			DisplayName: displayName,
//			BillingYen:  0,
//		},
//	}
//	return c.JSON(http.StatusOK, SuccessResult{Status: true, Data: res})
//}
