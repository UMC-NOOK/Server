variable "name" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "subnet_ids" {
  description = "At least two private subnet IDs in different AZs."
  type        = list(string)

  validation {
    condition     = length(var.subnet_ids) >= 2
    error_message = "RDS requires at least two private subnet IDs."
  }
}

variable "application_security_group_id" {
  type = string
}

variable "database_name" {
  type = string
}

variable "database_username" {
  type = string
}

variable "instance_class" {
  type    = string
  default = "db.t3.micro"
}

variable "master_user_secret_kms_key_id" {
  description = "Optional KMS key ID or ARN for the RDS-managed master password secret."
  type        = string
  default     = null
}

variable "allocated_storage" {
  type    = number
  default = 20
}

variable "deletion_protection" {
  type    = bool
  default = true
}

variable "skip_final_snapshot" {
  type    = bool
  default = false
}

variable "tags" {
  type    = map(string)
  default = {}
}
