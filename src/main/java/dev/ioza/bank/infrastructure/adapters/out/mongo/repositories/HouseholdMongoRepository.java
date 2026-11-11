package dev.ioza.bank.infrastructure.adapters.out.mongo.repositories;

import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.HouseholdDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HouseholdMongoRepository extends MongoRepository<HouseholdDoc, String> {}
