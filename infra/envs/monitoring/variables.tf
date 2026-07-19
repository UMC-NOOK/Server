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
  default = 30
}

variable "admin_cidrs" {
  description = "CIDRs allowed to access Grafana and optional SSH."
  type        = list(string)
  default     = []
}

variable "application_cidrs" {
  description = "Private CIDRs of dev/prod servers allowed to push logs to Loki."
  type        = list(string)
  default     = []
}
