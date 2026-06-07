package com.cablepulse.security;

import com.cablepulse.model.ApplicationSubscriptionTier;
import com.cablepulse.model.OperatorCompany;
import com.cablepulse.repository.ApplicationSubscriptionTierRepository;
import com.cablepulse.repository.OperatorCompanyRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class TenantActivationWorkflowFilter implements Filter {

    private final OperatorCompanyRepository operatorCompanyRepository;
    private final ApplicationSubscriptionTierRepository subscriptionTierRepository;

    @Autowired
    public TenantActivationWorkflowFilter(
            OperatorCompanyRepository operatorCompanyRepository,
            ApplicationSubscriptionTierRepository subscriptionTierRepository) {
        this.operatorCompanyRepository = operatorCompanyRepository;
        this.subscriptionTierRepository = subscriptionTierRepository;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization if needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // Match registration or activation endpoints specifically if needed
        String path = httpRequest.getRequestURI();
        if (path.startsWith("/api/v1/auth/register-tenant") || path.startsWith("/api/v1/tenants/activate")) {
            // Placeholder execution or interception hook for workflow logic validation
        }

        chain.doFilter(request, response);
    }

    /**
     * Executes the Tenant Activation pricing and promotional loop logic.
     * If total registered active platform tenants is under 100, assigns a 3-month free trial.
     * Otherwise, defaults to the active subscription tier's discounted pricing structure.
     */
    public void processTenantActivation(OperatorCompany company) {
        long totalRegisteredTenants = operatorCompanyRepository.count();

        if (totalRegisteredTenants < 100) {
            company.setPromotionalTrialActive(true);
            company.setTrialEndDate(LocalDateTime.now().plusMonths(3));
            company.setCurrentBillingAmount(0.00);
        } else {
            company.setPromotionalTrialActive(false);
            company.setTrialEndDate(null);
            
            // Fall back smoothly to the active subscription card's discountedPrice match
            ApplicationSubscriptionTier tier = company.getActiveSubscriptionTier();
            if (tier != null) {
                company.setCurrentBillingAmount(tier.getDiscountedPrice());
            } else {
                company.setCurrentBillingAmount(0.00); // Default fallback if no tier selected
            }
        }
        
        operatorCompanyRepository.save(company);
    }
}
