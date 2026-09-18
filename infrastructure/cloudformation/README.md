# AWS CloudFormation Deployment Guide
## Book Corner Platform

This directory contains production-ready, modular AWS CloudFormation templates for provisioning the serverless containerized architecture of the **Book Corner** Spring Boot application.

---

## Directory Structure

```
infrastructure/cloudformation/
├── 01-ecr-repository.yaml           # Amazon ECR with Inspector scanning & lifecycle policies
├── 02-iam-roles.yaml                # Least-privilege IAM roles (Access, Instance, RDS Monitoring)
├── 03-secrets-manager.yaml          # AWS KMS CMK & Secrets Manager (RDS credentials, JWT secret)
├── 04-rds-postgresql.yaml           # Amazon RDS PostgreSQL (Multi-AZ, GP3 auto-scaling, TLS 1.3)
├── 05-cloudwatch-logs.yaml          # CloudWatch Log Groups, Metric Alarms & Executive Dashboard
├── 06-apprunner-service.yaml        # AWS App Runner Service with VPC Connector & X-Ray tracing
├── bookcorner-complete-infrastructure.yaml  # Master all-in-one template
└── README.md                        # Deployment instructions & CLI commands
```

---

## Deployment Options

### Option A: Single-Stack Deployment (`bookcorner-complete-infrastructure.yaml`)

Deploy the entire production infrastructure in one command:

```bash
aws cloudformation create-stack \
  --stack-name book-corner-prod \
  --template-body file://bookcorner-complete-infrastructure.yaml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameters \
      ParameterKey=Environment,ParameterValue=prod \
      ParameterKey=AppName,ParameterValue=book-corner \
      ParameterKey=VpcId,ParameterValue=vpc-0123456789abcdef0 \
      ParameterKey=AppSubnetIds,ParameterValue=\"subnet-01111111111111111\,subnet-02222222222222222\" \
      ParameterKey=DbSubnetIds,ParameterValue=\"subnet-03333333333333333\,subnet-04444444444444444\" \
      ParameterKey=ContainerImageTag,ParameterValue=latest \
      ParameterKey=AppRunnerCpu,ParameterValue=\"2 vCPU\" \
      ParameterKey=AppRunnerMemory,ParameterValue=\"4 GB\" \
      ParameterKey=DBInstanceClass,ParameterValue=db.m6g.large
```

---

### Option B: Layered Modular Deployment (Recommended for GitOps / CI/CD)

Deploy each stack in sequence:

#### 1. Deploy ECR Repository
```bash
aws cloudformation create-stack \
  --stack-name book-corner-prod-ecr \
  --template-body file://01-ecr-repository.yaml \
  --parameters \
      ParameterKey=Environment,ParameterValue=prod \
      ParameterKey=AppName,ParameterValue=book-corner \
      ParameterKey=TagMutability,ParameterValue=IMMUTABLE
```

#### 2. Deploy IAM Roles
```bash
aws cloudformation create-stack \
  --stack-name book-corner-prod-iam \
  --template-body file://02-iam-roles.yaml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameters \
      ParameterKey=Environment,ParameterValue=prod \
      ParameterKey=AppName,ParameterValue=book-corner
```

#### 3. Deploy KMS Key & Secrets Manager
```bash
aws cloudformation create-stack \
  --stack-name book-corner-prod-secrets \
  --template-body file://03-secrets-manager.yaml \
  --parameters \
      ParameterKey=Environment,ParameterValue=prod \
      ParameterKey=AppName,ParameterValue=book-corner
```

#### 4. Deploy Amazon RDS PostgreSQL
```bash
aws cloudformation create-stack \
  --stack-name book-corner-prod-rds \
  --template-body file://04-rds-postgresql.yaml \
  --parameters \
      ParameterKey=Environment,ParameterValue=prod \
      ParameterKey=AppName,ParameterValue=book-corner \
      ParameterKey=VpcId,ParameterValue=vpc-0123456789abcdef0 \
      ParameterKey=DbSubnetIds,ParameterValue=\"subnet-03333333333333333\,subnet-04444444444444444\" \
      ParameterKey=AppRunnerSecurityGroupId,ParameterValue=sg-05555555555555555 \
      ParameterKey=DatabaseSecretArn,ParameterValue=arn:aws:secretsmanager:us-east-1:123456789012:secret:book-corner/prod/rds-XXXXXX \
      ParameterKey=KmsKeyArn,ParameterValue=arn:aws:kms:us-east-1:123456789012:key/XXXX-XXXX \
      ParameterKey=DBInstanceClass,ParameterValue=db.m6g.large
```

#### 5. Deploy CloudWatch Observability & Alarms
```bash
aws cloudformation create-stack \
  --stack-name book-corner-prod-observability \
  --template-body file://05-cloudwatch-logs.yaml \
  --parameters \
      ParameterKey=Environment,ParameterValue=prod \
      ParameterKey=AppName,ParameterValue=book-corner \
      ParameterKey=AppRunnerServiceName,ParameterValue=book-corner-prod-service \
      ParameterKey=RDSInstanceIdentifier,ParameterValue=book-corner-prod-postgres \
      ParameterKey=LogRetentionDays,ParameterValue=30
```

#### 6. Deploy AWS App Runner Service
```bash
aws cloudformation create-stack \
  --stack-name book-corner-prod-apprunner \
  --template-body file://06-apprunner-service.yaml \
  --parameters \
      ParameterKey=Environment,ParameterValue=prod \
      ParameterKey=AppName,ParameterValue=book-corner \
      ParameterKey=ImageUri,ParameterValue=123456789012.dkr.ecr.us-east-1.amazonaws.com/book-corner-prod-api:v1.0.0 \
      ParameterKey=VpcId,ParameterValue=vpc-0123456789abcdef0 \
      ParameterKey=AppSubnetIds,ParameterValue=\"subnet-01111111111111111\,subnet-02222222222222222\" \
      ParameterKey=AppRunnerAccessRoleArn,ParameterValue=arn:aws:iam::123456789012:role/book-corner-prod-AppRunnerAccessRole \
      ParameterKey=AppRunnerInstanceRoleArn,ParameterValue=arn:aws:iam::123456789012:role/book-corner-prod-AppRunnerInstanceRole \
      ParameterKey=DBEndpointAddress,ParameterValue=book-corner-prod-postgres.c123456789.us-east-1.rds.amazonaws.com \
      ParameterKey=DatabaseSecretArn,ParameterValue=arn:aws:secretsmanager:us-east-1:123456789012:secret:book-corner/prod/rds-XXXXXX \
      ParameterKey=JwtSecretArn,ParameterValue=arn:aws:secretsmanager:us-east-1:123456789012:secret:book-corner/prod/jwt-XXXXXX
```
