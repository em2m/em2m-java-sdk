# TODO


## Constraints

### General
- [ ] Variable substitution
- [ ] Wildcard patterns


### Types

#### String
- [ ] StringEquals
- [ ] StringEqualsIgnoreCase
- [ ] StringNotEquals
- [ ] StringNotEqualsIgnoreCase
- [ ] StringLike
- [ ] StringNotLike

#### Numeric

- [ ] NumericEquals
- [ ] NumericNotEquals
- [ ] NumericLessThan
- [ ] NumericLessThanEquals
- [ ] NumericGreaterThan
- [ ] NumericGreaterThanEquals

#### Date

- [ ] DateEquals
- [ ] DateNotEquals
- [ ] DateLessThan
- [ ] DateLessThanEquals
- [ ] DateGreaterThan
- [ ] DateGreaterThanEquals

#### Other

- [ ] Bool
- [ ] BinaryEquals
- [ ] IpAddress
- [ ] NotIpAddress


#### Arn

- [ ] ArnEquals
- [ ] ArnNotEquals
- [ ] ArnLike
- [ ] ArnNotLike

# Modifiers

- [ ] IfExists
- [ ] ForAllValues
- [ ] ForAnyValue


### Keys

#### Environment

- [x] aws:CurrentTime
- [x] aws:EpochTime
- [ ] env:Token  
- [ ] aws:TokenIssueTime
- [ ] aws:MultiFactorAuthPresent
- [ ] aws:MultiFactorAuthAge
- [ ] aws:PrincipalType
- [x] aws:Referer
- [ ] aws:SecureTransport
- [ ] aws:SourceArn
- [x] aws:SourceIp
- [ ] aws:UserAgent
- [ ] aws:SourceVpc
- [ ] aws:userid
- [ ] aws:username
- [ ] iam:PolicyArn
- [ ] aws:FederatedProvider