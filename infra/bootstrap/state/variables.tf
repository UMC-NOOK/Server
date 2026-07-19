variable "aws_region" {
  type    = string
  default = "ap-northeast-2"
}

variable "aws_account_id" {
  type = string
}

variable "state_bucket_name" {
  description = "Globally unique S3 bucket used by the environment backends."
  type        = string
}
