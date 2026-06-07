package com.cablepulse.service;

import com.cablepulse.dto.DtoClasses.StandardResponse_DailyCashSummaryData;
import com.cablepulse.dto.DtoClasses.StandardResponse_ExpenseCreated;
import com.cablepulse.dto.DtoClasses.StandardResponse_SettlementCreated;
import com.cablepulse.model.DailyExpense;
import com.cablepulse.model.IspSettlement;

import java.time.LocalDate;

public interface DailyLedgerService {

    StandardResponse_ExpenseCreated saveExpense(DailyExpense expense);

    StandardResponse_SettlementCreated saveIspSettlement(IspSettlement settlement);

    StandardResponse_DailyCashSummaryData getDailySummary(LocalDate targetDate);
}
