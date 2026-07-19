variable "aws_region" {
  type    = string
  default = "ap-northeast-2"
}

variable "aws_account_id" {
  type = string
}

variable "project_name" {
  type    = string
  default = "nook"
}

variable "vpc_id" {
  type = string
}

variable "public_subnet_id" {
  type = string
}

variable "private_db_subnet_ids" {
  type = list(string)
}

variable "ami_id" {
  type = string
}

variable "instance_type" {
  type    = string
  default = "t3.small"
}

variable "key_name" {
  type    = string
  default = null
}

variable "create_eip" {
  type    = bool
  default = true
}

variable "root_volume_size" {
  type    = number
  default = 20
}

variable "admin_cidrs" {
  type    = list(string)
  default = []
}

variable "monitoring_cidrs" {
  description = "Private CIDR of the monitoring server allowed to scrape port 9091."
  type        = list(string)
  default     = []
}

variable "database_name" {
  type    = string
  default = "nookDB"
}

variable "database_username" {
  type    = string
  default = "nook"
}

variable "database_instance_class" {
  type    = string
  default = "db.t3.micro"
}

variable "master_user_secret_kms_key_id" {
  description = "Optional KMS key for the RDS-managed password in Secrets Manager."
  type        = string
  default     = null
}
