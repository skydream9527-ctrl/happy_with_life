package ledger

type Store interface {
	EnsurePersonalSpace(userID, name string) (Space, error)
	ListSpacesForUser(userID string) ([]Space, error)
	GetSpace(id string) (*Space, error)
	GetMember(spaceID, userID string) (*Member, error)
	GetRecord(id string) (*Record, error)
	LiveRecords(spaceID string) ([]Record, error)
	ListRecords(spaceID, afterOccurred, afterID string, limit int) ([]Record, error)
	ListChanges(userID string, afterSeq int64, limit int) ([]Change, error)
	ApplyTx(fn func(tx Tx) error) error
	CreateSpace(sp Space, owner Member) error
	UpdateSpace(sp Space) error
	ListMembers(spaceID string) ([]Member, error)
	SaveMember(m Member) error
	CountActiveMembers(spaceID string) (int, error)
	InsertInvite(inv Invite) error
	GetInviteByHash(hash string) (*Invite, error)
	GetInvite(id string) (*Invite, error)
	UpdateInvite(inv Invite) error
	ListInvites(spaceID string) ([]Invite, error)
}

type Tx interface {
	GetMember(spaceID, userID string) (*Member, error)
	GetSpace(id string) (*Space, error)
	GetRecord(id string) (*Record, error)
	LiveRecords(spaceID string) ([]Record, error)
	UpsertRecord(r Record) error
	LockDailyStats(spaceID, date string) (DailyStats, error)
	SaveDailyStats(s DailyStats) error
	SetSpaceTotals(spaceID string, totalGP int64, stage string) error
	NextSeq() (int64, error)
	InsertChange(c Change) error
	GetMutation(id string) (*AppliedMutation, error)
	SaveMutation(m AppliedMutation) error
}
