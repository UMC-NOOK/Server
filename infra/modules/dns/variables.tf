variable "domain" {
  description = "Apex domain name (e.g. 'booknook.work')."
  type        = string
}

variable "create_zone" {
  description = "Create a new Route53 hosted zone. Set false to import an existing one."
  type        = bool
  default     = true
}

# Map key is a logical label; value carries the subdomain and IP.
# Example:
#   a_records = {
#     api = { name = "api", ip = "1.2.3.4" }
#     dev = { name = "dev", ip = "5.6.7.8" }
#   }
variable "a_records" {
  description = "A records to create. name is the subdomain (empty string for apex)."
  type = map(object({
    name = string
    ip   = string
  }))
  default = {}
}

variable "ttl" {
  description = "TTL in seconds for all A records."
  type        = number
  default     = 300
}

variable "tags" {
  description = "Additional AWS tags."
  type        = map(string)
  default     = {}
}
