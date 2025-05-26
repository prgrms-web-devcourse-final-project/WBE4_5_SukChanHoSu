terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
  }
}

provider "aws" {
  region = var.region
}

resource "aws_vpc" "vpc-01" {
  cidr_block = "10.0.0.0/16"

  tags = {
    Name = "${var.prefix}-vpc-01"
    Team = var.Team
  }
}

resource "aws_subnet" "subnet-01" {
  vpc_id                  = aws_vpc.vpc-01.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "${var.region}a"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.prefix}-subnet-01"
    Team = var.Team
  }
}

resource "aws_subnet" "subnet-02" {
  vpc_id                  = aws_vpc.vpc-01.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = "${var.region}a"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.prefix}-subnet-02"
    Team = var.Team
  }
}

resource "aws_internet_gateway" "igw-01" {
  vpc_id = aws_vpc.vpc-01.id

  tags = {
    Name = "${var.prefix}-igw-01"
    Team = var.Team
  }
}

resource "aws_route_table" "rt-01" {
  vpc_id = aws_vpc.vpc-01.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw-01.id
  }

  tags = {
    Name = "${var.prefix}-rt-1"
    Team = var.Team
  }
}

resource "aws_route_table_association" "association-01" {
  subnet_id      = aws_subnet.subnet-01.id
  route_table_id = aws_route_table.rt-01.id
}

resource "aws_route_table_association" "association_2" {
  subnet_id      = aws_subnet.subnet-02.id
  route_table_id = aws_route_table.rt-01.id
}

resource "aws_security_group" "nginx-sg" {
  name   = "nginx-proxy-sg"
  vpc_id = aws_vpc.vpc-01.id

  ingress {
    description = "DB"
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Allow HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Spring Boot"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Spring Boot2"
    from_port   = 8081
    to_port     = 8081
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "haproxy"
    from_port   = 8090
    to_port     = 8090
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Allow HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "NPM Admin UI Port 81"
    from_port   = 81
    to_port     = 81
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Grafana"
    from_port   = 3000
    to_port     = 3000
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Prometheus"
    from_port   = 9090
    to_port     = 9090
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Redis Exporter"
    from_port   = 9121
    to_port     = 9121
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Redis"
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.prefix}-nginx-proxy-sg"
    Team = var.Team
  }
}

resource "aws_iam_role" "ec2_role-01" {
  name = "${var.prefix}-ec2-role-01"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Action = "sts:AssumeRole",
        Effect = "Allow",
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "s3_full_access" {
  role       = aws_iam_role.ec2_role-01.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

resource "aws_iam_role_policy_attachment" "ec2_ssm" {
  role       = aws_iam_role.ec2_role-01.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2RoleforSSM"
}

resource "aws_s3_bucket" "app_bucket" {
  bucket = var.bucket_name

  tags = {
    Name = "${var.prefix}-profile-image-bucket"
    Team = var.Team
  }
}

resource "aws_s3_bucket_public_access_block" "app_bucket_block" {
  bucket = aws_s3_bucket.app_bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_iam_policy" "ec2_app_s3_policy" {
  name        = "${var.prefix}-ec2-profile-image-policy"
  description = "EC2가 profile-image S3 버킷에 접근할 수 있는 권한"

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect = "Allow",
        Action = ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"],
        Resource = "${aws_s3_bucket.app_bucket.arn}/*"
      },
      {
        Effect = "Allow",
        Action = ["s3:ListBucket"],
        Resource = aws_s3_bucket.app_bucket.arn
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ec2_app_s3_policy_attachment" {
  role       = aws_iam_role.ec2_role-01.name
  policy_arn = aws_iam_policy.ec2_app_s3_policy.arn
}

resource "aws_eip" "eip_ec2_1" {
  tags = {
    Name = "${var.prefix}-eip-ec2-1"
    Team = var.Team
  }
}

resource "aws_iam_instance_profile" "instance_profile_01" {
  name = "${var.prefix}-instance-profile-1"
  role = aws_iam_role.ec2_role-01.name
}

resource "aws_instance" "ec2_1" {
  ami                         = data.aws_ami.latest_amazon_linux.id
  instance_type               = "t3.micro"
  subnet_id                   = aws_subnet.subnet-02.id
  vpc_security_group_ids      = [aws_security_group.nginx-sg.id]
  associate_public_ip_address = true
  iam_instance_profile        = aws_iam_instance_profile.instance_profile_01.name

  tags = {
    Name = "${var.prefix}-ec2-1"
    Team = var.Team
  }

  root_block_device {
    volume_type = "gp3"
    volume_size = 12
  }

  user_data = file("${path.module}/scripts/user_data.sh.tpl")
}

resource "aws_instance" "ec2_2" {
  ami                         = data.aws_ami.latest_amazon_linux.id
  instance_type               = "t3.small"
  subnet_id                   = aws_subnet.subnet-01.id
  vpc_security_group_ids      = [aws_security_group.nginx-sg.id]
  associate_public_ip_address = true
  iam_instance_profile        = aws_iam_instance_profile.instance_profile_01.name

  tags = {
    Name = "${var.prefix}-ec2-2"
    Team = var.Team
  }

  root_block_device {
    volume_type = "gp3"
    volume_size = 12
  }

  user_data = file("${path.module}/scripts/user_data.sh.tpl")
}

resource "aws_eip_association" "eip_assoc_ec2_1" {
  instance_id   = aws_instance.ec2_2.id
  allocation_id = aws_eip.eip_ec2_1.id
}

data "aws_ami" "latest_amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  filter {
    name   = "root-device-type"
    values = ["ebs"]
  }
}
