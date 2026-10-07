package pe.sotomayor.creditos.credit.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.sotomayor.creditos.credit.domain.service.CreditCalculator;
import pe.sotomayor.creditos.credit.domain.service.credit.CreditCodeGenerator;
import pe.sotomayor.creditos.credit.domain.service.credit.CreditNumberGenerator;
import pe.sotomayor.creditos.credit.domain.service.credit.CreditSequenceProvider;

import pe.sotomayor.creditos.credit.domain.service.DefaultCreditCalculator;

import pe.sotomayor.creditos.credit.domain.service.credit.SequentialCreditNumberGenerator;
import pe.sotomayor.creditos.credit.domain.service.credit.UuidCreditCodeGenerator;

import pe.sotomayor.creditos.credit.domain.service.creditapplication.*;

/**
 * Configura los servicios de dominio utilizados por el módulo de créditos.
 *
 * @author DarwinG
 */
@Configuration
public class CreditDomainConfig {

    @Bean
    public CreditCalculator creditCalculator() {
        return new DefaultCreditCalculator();
    }

    @Bean
    public CreditCodeGenerator creditCodeGenerator() {
        return new UuidCreditCodeGenerator();
    }

    @Bean
    public CreditNumberGenerator creditNumberGenerator(
            CreditSequenceProvider sequenceProvider) {

        return new SequentialCreditNumberGenerator(
                sequenceProvider
        );
    }

    @Bean
    public CreditApplicationCodeGenerator
    creditApplicationCodeGenerator() {
        return new UuidCreditApplicationCodeGenerator();
    }

    @Bean
    public CreditApplicationNumberGenerator creditApplicationNumberGenerator(
            CreditApplicationSequenceProvider sequenceProvider) {

        return new SequentialCreditApplicationNumberGenerator(
                sequenceProvider
        );
    }

}