package com.fincard.audit.service;

import com.fincard.audit.dto.TransactionApprovedMessage;

public interface IAuditService {

    public boolean register(TransactionApprovedMessage message);
}