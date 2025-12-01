package com.healthrx.contest_solver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.healthrx.contest_solver.dto.InitialRequest;
import com.healthrx.contest_solver.dto.SubmissionRequest;
import com.healthrx.contest_solver.dto.WebhookResponse;

@Service
public class ContestService {

    private static final Logger logger = LoggerFactory.getLogger(ContestService.class);

    private final RestTemplate restTemplate;

    @Value("${contest.api.generate-url}")
    private String generateUrl;

    @Value("${contest.user.name}")
    private String userName;

    @Value("${contest.user.regNo}")
    private String userRegNo;

    @Value("${contest.user.email}")
    private String userEmail;

    private static final String ODD_SQL_SOLUTION = """
        SELECT dt.DEPARTMENT_NAME, dt.SALARY, dt.EMPLOYEE_NAME, dt.AGE 
        FROM (
            SELECT 
                d.DEPARTMENT_NAME, 
                SUM(p.AMOUNT) AS SALARY, 
                CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS EMPLOYEE_NAME, 
                TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE, 
                RANK() OVER (PARTITION BY d.DEPARTMENT_ID ORDER BY SUM(p.AMOUNT) DESC) as rnk 
            FROM DEPARTMENT d 
            JOIN EMPLOYEE e ON d.DEPARTMENT_ID = e.DEPARTMENT 
            JOIN PAYMENTS p ON e.EMP_ID = p.EMP_ID 
            WHERE DAY(p.PAYMENT_TIME) != 1 
            GROUP BY d.DEPARTMENT_ID, e.EMP_ID, d.DEPARTMENT_NAME, e.FIRST_NAME, e.LAST_NAME, e.DOB
        ) dt 
        WHERE dt.rnk = 1
        """;

    private static final String EVEN_SQL_SOLUTION = "SELECT count(*) FROM doctors"; 

    public ContestService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void executeContestFlow() {
        logger.info(">>> Starting Contest Flow...");

        WebhookResponse webhookData = generateWebhook();
        
        if (webhookData == null) {
            logger.error("Failed to retrieve webhook data. Aborting.");
            return;
        }
        if (webhookData.getAccessToken() == null) {
            logger.error("ERROR: Access Token is NULL. Check if API response format changed.");
            return;
        }

        String targetUrl = webhookData.getWebhook();
        if (targetUrl == null) {
             logger.error("ERROR: Webhook URL is NULL. The API did not return 'webhook' or 'webhookUrl'.");
             return;
        }

        logger.info("Received Token: {}", webhookData.getAccessToken());
        logger.info("Received Webhook URL: {}", targetUrl);

        String solutionQuery = solveProblem(userRegNo);
        logger.info("Selected Solution: {}", solutionQuery);

        submitSolution(targetUrl, webhookData.getAccessToken(), solutionQuery);
        
        logger.info(">>> Contest Flow Completed.");
    }

    private WebhookResponse generateWebhook() {
        try {
            logger.info("Calling Generate Webhook API: {}", generateUrl);
            
            InitialRequest request = new InitialRequest(userName, userRegNo, userEmail);
            HttpEntity<InitialRequest> entity = new HttpEntity<>(request);

            ResponseEntity<WebhookResponse> response = restTemplate.postForEntity(
                generateUrl, 
                entity, 
                WebhookResponse.class
            );

            return response.getBody();

        } catch (Exception e) {
            logger.error("Error during webhook generation: ", e);
            return null;
        }
    }

    private String solveProblem(String regNo) {
        int lastTwoDigits = 0;
        try {
            String digits = regNo.replaceAll("[^0-9]", ""); 
            if (digits.length() >= 2) {
                lastTwoDigits = Integer.parseInt(digits.substring(digits.length() - 2));
            }
        } catch (Exception e) {
            logger.warn("Could not parse regNo, defaulting to ODD logic.");
            lastTwoDigits = 1; 
        }

        logger.info("RegNo: {}, Last Digits: {}", regNo, lastTwoDigits);

        if (lastTwoDigits % 2 != 0) {
            logger.info("Result is ODD. Selecting Question 1 Solution.");
            return ODD_SQL_SOLUTION;
        } else {
            logger.info("Result is EVEN. Selecting Question 2 Solution.");
            return EVEN_SQL_SOLUTION;
        }
    }

    private void submitSolution(String url, String token, String query) {
        try {
            logger.info("Submitting solution to: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", token); 
            
            SubmissionRequest submission = new SubmissionRequest(query);
            HttpEntity<SubmissionRequest> entity = new HttpEntity<>(submission, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            logger.info("Submission Response Code: {}", response.getStatusCode());
            logger.info("Submission Response Body: {}", response.getBody());
            
        } catch (Exception e) {
            logger.error("Error during submission: ", e);
        }
    }
}