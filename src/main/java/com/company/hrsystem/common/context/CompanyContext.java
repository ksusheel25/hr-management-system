package com.company.hrsystem.common.context;

import java.util.Optional;
import java.util.UUID;

public final class CompanyContext {

    private static final ThreadLocal<UUID> COMPANY_ID_HOLDER = new ThreadLocal<>();

    private CompanyContext() {
    }

    public static void setCompanyId(UUID companyId) {
        COMPANY_ID_HOLDER.set(companyId);
    }

    public static Optional<UUID> getCompanyId() {
        return Optional.ofNullable(COMPANY_ID_HOLDER.get());
    }

    public static void clear() {
        COMPANY_ID_HOLDER.remove();
    }
}
