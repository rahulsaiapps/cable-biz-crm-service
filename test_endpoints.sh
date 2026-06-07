#!/bin/bash

export JWT_TOKEN="eyJhbGciOiJSUzI1NiIsImtpZCI6Ijc5OTRiNGYzMTU2MzJiMjk3NzAwNmQ5M2U5NGIyYWNiZTMwNWZlNDYiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL3NlY3VyZXRva2VuLmdvb2dsZS5jb20vY2FibGUtYml6LWNybSIsImF1ZCI6ImNhYmxlLWJpei1jcm0iLCJhdXRoX3RpbWUiOjE3ODA4NDI2NTAsInVzZXJfaWQiOiJWdzV0THZHZzZyZ1dqdGhCNjU0elkyZW9WTEkyIiwic3ViIjoiVnc1dEx2R2c2cmdXanRoQjY1NHpZMmVvVkxJMiIsImlhdCI6MTc4MDg0MjY1MiwiZXhwIjoxNzgwODQ2MjUyLCJlbWFpbCI6InJhaHVsLnNhaUBnbWFpbC5jb20iLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsImZpcmViYXNlIjp7ImlkZW50aXRpZXMiOnsiZW1haWwiOlsicmFodWwuc2FpQGdtYWlsLmNvbSJdfSwic2lnbl9pbl9wcm92aWRlciI6InBhc3N3b3JkIn19.TWVkkXBbunhtAqvoypu3GcsCejPKTfjTq89RFGKTDKeEYTw14XAjflUf0XdcLT18OciP6ROI2FJg06RFO7N6jEdePdOZAOTT4f9OvPrOoGSO9aTnLu_xMCChj15jZRoECDz0Gu9p2fHd-coftnEdgkHfYEgi77imyZ201wXr9g-Sr3GBtqIytJE1fUTU3_L1JivcIPeRXiBulKW6duIh47YkbPVPwe_s2D7dsh19os5UF2VkjJmtFZhyTzF-S274h0VI9h_rOzO56xCIWNXMtwtYGUdcd6g8ZVgzTlu_OkpG3n9gMxU66Y2r6jP_paJajrPf_F58BUTLuYvTd5EX-w"
export E2E_ID="9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"
export SESSION_ID="1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d"
export BASE_URL="http://localhost:8080/api/v1"

echo "=================================="
echo "TEST IDENTITY SWAP GATEWAY"
echo "=================================="
curl -X POST "$BASE_URL/auth/token-swap" \
  -H "Content-Type: application/json" \
  -d "{\"firebaseIdToken\":\"$JWT_TOKEN\"}"
echo -e "\n"

echo "=================================="
echo "TERRITORY WORKSPACE CUSTOMERS FEED"
echo "=================================="
curl -X GET "$BASE_URL/workspace/customers?locationId=vil_kolamuru_001" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "X-E2E-ID: $E2E_ID" \
  -H "X-Session-ID: $SESSION_ID"
echo -e "\n"

echo "=================================="
echo "TRI-LEDGER SUMMARY MATRIX ENDPOINT"
echo "=================================="
curl -X GET "$BASE_URL/transactions/daily-summary?targetDate=2026-06-07" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "X-E2E-ID: $E2E_ID" \
  -H "X-Session-ID: $SESSION_ID"
echo -e "\n=================================="
