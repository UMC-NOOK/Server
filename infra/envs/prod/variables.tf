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

variable "domain_name" {
  description = "Public hostname for the production server."
  type        = string
  default     = "api.booknook.work"
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
