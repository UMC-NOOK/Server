variable "name" {
  description = "Resource name prefix."
  type        = string
}

variable "aws_region" {
  description = "AWS region. Subnets are placed in the 'a' AZ of this region."
  type        = string
  default     = "ap-northeast-2"
}

variable "vpc_cidr" {
  description = "IPv4 CIDR block for the VPC."
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_a_cidr" {
  description = "CIDR for the public subnet in AZ 'a'."
  type        = string
  default     = "10.0.1.0/24"
}
