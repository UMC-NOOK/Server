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

# ── Network ──────────────────────────────────────────────────────────────────

variable "vpc_cidr" {
  description = "CIDR block for the prod VPC."
  type        = string
  default     = "10.1.0.0/16"
}

variable "public_subnet_a_cidr" {
  description = "CIDR for the public subnet in AZ 'a'."
  type        = string
  default     = "10.1.1.0/24"
}

# ── Compute ──────────────────────────────────────────────────────────────────

variable "ami_id" {
  description = "Ubuntu 22.04 LTS AMI ID for the production EC2."
  type        = string
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
  description = "Private CIDR of the monitoring server allowed to scrape ports 9091/9121."
  type        = list(string)
  default     = []
}

# ── DNS (Route53) ─────────────────────────────────────────────────────────────

variable "enable_route53" {
  description = "Create Route53 records. Enable after pointing the registrar at Route53 name servers."
  type        = bool
  default     = false
}

variable "create_route53_zone" {
  description = "Create a new hosted zone. Set false to reuse an existing zone in this account."
  type        = bool
  default     = true
}

variable "route53_zone" {
  description = "Apex domain managed in Route53 (e.g. 'booknook.work')."
  type        = string
  default     = "booknook.work"
}

variable "domain_subdomain" {
  description = "Subdomain for the prod server A record (e.g. 'api' → api.booknook.work)."
  type        = string
  default     = "api"
}

# Kept for documentation/output only; not used in resource definitions.
variable "domain_name" {
  description = "Full public hostname of the production server."
  type        = string
  default     = "api.booknook.work"
}
