variable "name" {
  description = "Resource name prefix."
  type        = string
}

variable "vpc_id" {
  description = "VPC that contains the instance."
  type        = string
}

variable "subnet_id" {
  description = "Subnet in which the instance is created."
  type        = string
}

variable "ami_id" {
  description = "AMI ID for the EC2 instance."
  type        = string
}

variable "instance_type" {
  description = "EC2 instance type."
  type        = string
  default     = "t3.small"
}

variable "key_name" {
  description = "Optional EC2 key pair name. Prefer SSM for server access."
  type        = string
  default     = null
}

variable "associate_public_ip_address" {
  description = "Whether the instance receives a public IP from the subnet."
  type        = bool
  default     = true
}

variable "create_eip" {
  description = "Whether to assign an Elastic IP."
  type        = bool
  default     = true
}

variable "root_volume_size" {
  description = "Root EBS volume size in GiB."
  type        = number
  default     = 20
}

variable "ingress_rules" {
  description = "Inbound rules. Restrict CIDRs to trusted networks."
  type = map(object({
    description = string
    from_port   = number
    to_port     = number
    protocol    = string
    cidr_blocks = list(string)
  }))
  default = {}
}

variable "additional_security_group_ids" {
  description = "Existing security groups kept during a gradual migration to Terraform."
  type        = list(string)
  default     = []
}

variable "user_data" {
  description = "Cloud-init script."
  type        = string
  default     = null
}

variable "tags" {
  description = "Additional AWS tags."
  type        = map(string)
  default     = {}
}
