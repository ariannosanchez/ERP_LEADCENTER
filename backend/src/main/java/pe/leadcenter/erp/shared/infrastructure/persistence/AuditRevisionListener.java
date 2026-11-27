package pe.leadcenter.erp.shared.infrastructure.persistence;

import org.hibernate.envers.RevisionListener;
import pe.leadcenter.erp.shared.security.CurrentUser;

/** Stamps each Envers revision with the authenticated user (null for system writes). */
public class AuditRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        ((AuditRevision) revisionEntity).setModifiedBy(CurrentUser.id().orElse(null));
    }
}
