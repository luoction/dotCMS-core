package com.dotmarketing.startup.runonce;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.startup.StartupTask;

import java.util.Map;

import static com.dotcms.util.CollectionsUtils.map;

/**
 * Remove the foreign key with the publishing_end_point table to all the Integrity Resolver tables and rename the column
 * endpoint_id to remote IP.
 *
 * The Integrity Resolver tables are:
 *
 * - cms_roles_ir
 * - folder_ir
 * - structures_ir
 * - htmlpages_ir
 * - fileassets_ir
 */
public class Task05390RemoveEndpointIdForeignKeyInIntegrityResolverTables implements StartupTask {
    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        final Map<String, String> tables = map(
                "folders_ir", "fk_folder_ir_ep",
                "structures_ir", "fk_structure_ir_ep",
                "htmlpages_ir", "fk_page_ir_ep",
                "fileassets_ir", "fk_file_ir_ep",
                "cms_roles_ir", "fk_cms_roles_ir_ep"
        );

        for (Map.Entry<String, String> entry : tables.entrySet()) {
            try {
                dropConstraint(entry.getKey(), entry.getValue());
            }catch (DotDataException e) {
                continue;
            }
        }

    }

    private void dropConstraint(final String tableName, final String constraintName) throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL(String.format("ALTER TABLE %s DROP CONSTRAINT %s", tableName, constraintName));
        dc.loadResult();
    }
}