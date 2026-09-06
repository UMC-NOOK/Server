terraform {
  required_version = ">= 1.10.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }

  # S3 remote backend — activate after running bootstrap/state to create the bucket.
  # terraform init -migrate-state  (migrates existing local state on first enable)
  #
  # backend "s3" {
  #   bucket         = "nook-terraform-state"
  #   key            = "envs/dev/terraform.tfstate"
  #   region         = "ap-northeast-2"
  #   dynamodb_table = "nook-terraform-lock"
  #   encrypt        = true
  # }
}

provider "aws" {
  region              = var.aws_region
  allowed_account_ids = [var.aws_account_id]

  default_tags {
    tags = local.common_tags
  }
}
