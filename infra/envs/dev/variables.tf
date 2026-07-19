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
  description = "Existing low-traffic development server size."
  type        = string
  default     = "t3.micro"
}

variable "key_name" {
  type    = string
  default = null
}

variable "create_eip" {
  description = "Set true for a stable public IP. Import the current EIP before apply when one exists."
  type        = bool
  default     = true
}

variable "root_volume_size" {
  type    = number
  default = 20
}

variable "existing_security_group_ids" {
  description = "Current security groups retained during the existing dev server migration."
  type        = list(string)
  default     = []
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
