package dev.ioza.bank.infrastructure.config;

import org.bson.Document;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.mongodb.ReadConcern;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;

@Configuration
@EnableTransactionManagement
public class InfrastructureConfig {
  @Bean
  MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory factory) {
    return new MongoTransactionManager(
        factory,
        TransactionOptions.builder()
            .readConcern(ReadConcern.SNAPSHOT)
            .writeConcern(WriteConcern.MAJORITY)
            .build());
  }

  @Bean
  ApplicationRunner requireTransactionalMongo(MongoDatabaseFactory factory) {
    return args -> {
      Document hello = factory.getMongoDatabase().runCommand(new Document("hello", 1));
      if (!hello.containsKey("setName") && !"isdbgrid".equals(hello.getString("msg"))) {
        throw new IllegalStateException(
            "BANK requiere MongoDB replica set o mongos; standalone no permite transacciones");
      }
    };
  }
}
