package com.hackathon.blockchain.service;

import com.hackathon.blockchain.model.SmartContract;
import com.hackathon.blockchain.model.Transaction;
import com.hackathon.blockchain.repository.SmartContractRepository;
import com.hackathon.blockchain.repository.TransactionRepository;
import com.hackathon.blockchain.utils.SignatureUtil;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.PublicKey;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SmartContractEvaluationService {

    private final SmartContractRepository smartContractRepository;
    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final WalletKeyService walletKeyService; // Para obtener la clave pública del emisor
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private static final Logger log = LoggerFactory.getLogger(SmartContractEvaluationService.class);

    public SmartContractEvaluationService(SmartContractRepository smartContractRepository,
                                          TransactionRepository transactionRepository,
                                          WalletService walletService,
                                          WalletKeyService walletKeyService) {
        this.smartContractRepository = smartContractRepository;
        this.transactionRepository = transactionRepository;
        this.walletService = walletService;
        this.walletKeyService = walletKeyService;
    }
    
    /**
     * Verifica la firma digital del contrato usando la clave pública del emisor.
     */
    public boolean verifyContractSignature(SmartContract contract) {
        try {
            PublicKey issuerPublicKey = walletKeyService.getPublicKeyForWallet(contract.getIssuerWalletId());
            if (issuerPublicKey == null) {
                return false;
            }
            String dataToSign = contract.getName() +
                                  contract.getConditionExpression() +
                                  contract.getAction() +
                                  contract.getActionValue() +
                                  contract.getIssuerWalletId();
            return SignatureUtil.verifySignature(dataToSign, contract.getDigitalSignature(), issuerPublicKey);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    /**
     * Evalúa todos los smart contracts activos sobre las transacciones pendientes.
     * Se inyectan las variables "amount" y "txType" en el contexto de SpEL.
     * Si la condición se cumple y la firma es válida, se ejecuta la acción definida:
     * - Para "CANCEL_TRANSACTION", se marca la transacción como "CANCELED".
     * - (Si hubiera otras acciones, se podrían implementar aquí).
     */
    @Transactional
    public void evaluateSmartContracts() {
        List<SmartContract> activeContracts = smartContractRepository.findByStatus("ACTIVE");
        List<Transaction> pendingTransactions = transactionRepository.findByStatus("PENDING");
        
        for (Transaction transaction : pendingTransactions) {
            boolean transactionUpdated = false;
            StandardEvaluationContext context = createEvaluationContext(transaction);
            
            for (SmartContract contract : activeContracts) {
                if (!isValidContract(contract) || !evaluateCondition(contract, context)) {
                    continue;
                }
                
                executeContractAction(contract, transaction);
                transactionUpdated = true;
                
                if ("CANCEL_TRANSACTION".equalsIgnoreCase(contract.getAction())) {
                    break; // Si se cancela, no procesar más contratos
                }
            }
            
            if (transactionUpdated) {
                transactionRepository.save(transaction);
            }
        }
    }

    private StandardEvaluationContext createEvaluationContext(Transaction tx) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("amount", tx.getAmount());
        context.setVariable("txType", tx.getType());
        context.setVariable("sender", tx.getSenderWallet().getAddress());
        context.setVariable("receiver", tx.getReceiverWallet().getAddress());
        return context;
    }

    private boolean isValidContract(SmartContract contract) {
        try {
            PublicKey publicKey = walletKeyService.getPublicKeyForWallet(contract.getIssuerWalletId());
            if (publicKey == null) return false;
            
            String dataToVerify = String.join("|", 
                contract.getName(),
                contract.getConditionExpression(),
                contract.getAction(),
                String.valueOf(contract.getActionValue()),
                contract.getIssuerWalletId()
            );
            
            return SignatureUtil.verifySignature(
                dataToVerify, 
                contract.getDigitalSignature(), 
                publicKey
            );
        } catch (Exception e) {
            log.error("Error validating contract {}: {}", contract.getId(), e.getMessage());
            return false;
        }
    }

    private boolean evaluateCondition(SmartContract contract, StandardEvaluationContext context) {
        try {
            Expression expression = parser.parseExpression(contract.getConditionExpression());
            Boolean result = expression.getValue(context, Boolean.class);
            return result != null && result;
        } catch (Exception e) {
            log.warn("Error evaluating condition for contract {}: {}", contract.getId(), e.getMessage());
            return false;
        }
    }

    private void executeContractAction(SmartContract contract, Transaction transaction) {
        switch (contract.getAction().toUpperCase()) {
            case "CANCEL_TRANSACTION":
                transaction.setStatus("CANCELED");
                log.info("Transaction {} canceled by contract {}", transaction.getId(), contract.getId());
                break;
                
            case "TRANSFER_FEE":
                if (walletService.transferFee(transaction, contract.getActionValue())) {
                    transaction.setStatus("PROCESSED_WITH_FEE");
                    log.info("Fee applied to transaction {}: {}", transaction.getId(), contract.getActionValue());
                }
                break;
                
            default:
                log.warn("Unknown contract action: {}", contract.getAction());
        }
    }
}