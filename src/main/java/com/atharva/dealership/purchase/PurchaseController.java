package com.atharva.dealership.purchase;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @GetMapping("/me")
    public ResponseEntity<List<PurchaseResponse>> myPurchases(Authentication authentication) {
        log.info("Received my purchases request");
        List<PurchaseResponse> purchases = purchaseService.findMyPurchases(authentication.getName()).stream()
                .map(PurchaseResponse::from)
                .toList();
        log.info("My purchases request handled successfully with {} purchases", purchases.size());
        return ResponseEntity.ok(purchases);
    }
}
