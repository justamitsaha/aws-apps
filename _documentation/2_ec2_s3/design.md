
# Target Architecture (After This Change)

```  
Local machine / CI
 ├─ build JARs
 └─ upload JARs to S3

EC2
 ├─ install Java only
 ├─ download JARs from S3
 └─ systemd runs apps

 ```  
No Git clone.    
No Maven on EC2.    
No build during boot.
## Step 1: One-Time: Create S3 Bucket (Manual or CLI)

Choose a unique name. `aws s3 mb s3://amit-app-artifacts --region us-east-1`

Folder layout in S3:

`s3://amit-app-artifacts/ ├── fileReader/  
 │    └── fileReader.jar  
 └── reporting/  
      └── reporting.jar`

## Step 2: Add permissions → **Create policy**

Click:  `Create  policy`

Choose:  `JSON`

Paste **this exact policy** (minimal and correct):

`{  "Version":  "2012-10-17",  "Statement":  [  {  "Effect":  "Allow",  "Action":  "s3:GetObject",  "Resource":  "arn:aws:s3:::amit-app-artifacts/*"  }  ]  }`

Then:
- Policy name: `EC2ReadArtifactsFromS3`
- Create policy

## Step 3 Create role → **Create role**
Select `AWS service` → `EC2` → `Next: Permissions` ->`Attach permissions policies`  
Use **exactly** this (important for scripts):

`EC2S3ReadRole`

Finish creation.

####  🧠 What AWS Does Automatically
When you create the role:
- AWS **also creates an Instance Profile**
- The instance profile has **the same name as the role**

The EC2 instance must be launched with the IAM instance profile EC2S3ReadRole.

## Step 4 upload the jar to s3
Run `upload-artifacts.sh` on your local machine or CI system, not on EC2.
This will upload the Jar to S3
## Step 5 Create Ec2
run `ec2-start.sh`

This will

1. Create EC2
2. Attach security groups
3. Attach IAM role
4. Run user data
5. Download jar from s3
6. start apps
7. create service
