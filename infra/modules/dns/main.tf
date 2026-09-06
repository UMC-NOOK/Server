# Route53 hosted zone — create new or reference an existing one.
# Set create_zone = false when the zone already exists in this account.
resource "aws_route53_zone" "this" {
  count = var.create_zone ? 1 : 0
  name  = var.domain
  tags  = var.tags
}

data "aws_route53_zone" "existing" {
  count = var.create_zone ? 0 : 1
  name  = var.domain
}

locals {
  zone_id = var.create_zone ? aws_route53_zone.this[0].zone_id : data.aws_route53_zone.existing[0].zone_id
}

resource "aws_route53_record" "a" {
  for_each = var.a_records

  zone_id = local.zone_id
  name    = each.value.name
  type    = "A"
  ttl     = var.ttl
  records = [each.value.ip]
}
